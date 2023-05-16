import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.sql.*;

import com.alibaba.fastjson.JSON;

import java.util.concurrent.ThreadLocalRandom;

public class server {
    private static final int BATCH_SIZE = 1000;
    private static Connection con = null;
    private ResultSet resultSet;
    private static PreparedStatement stmtPost = null;
    private static PreparedStatement stmtAuthor = null;
    private static PreparedStatement stmtFollow = null;
    private static PreparedStatement stmtFavr = null;
    private static PreparedStatement stmtShare = null;
    private static PreparedStatement stmtLike = null;
    private static PreparedStatement stmtCate = null;
    private static PreparedStatement stmtReply = null;
    private static PreparedStatement stmtSecondReply = null;
    private static PreparedStatement stmtReToSecRe = null;
    static int cnt = 0;
    static int replyId = 1;
    static int secondReplyId = 1;
    static HashMap<Integer, String> PostTimeMap = new HashMap<>();

    private static void openDB(Properties prop) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (Exception e) {
            System.err.println("Cannot find the Postgres driver. Check CLASSPATH.");
            System.exit(1);
        }
        String url = "jdbc:postgresql://" + prop.getProperty("host") + "/" + prop.getProperty("database");
        try {
            con = DriverManager.getConnection(url, prop);
            if (con != null) {
                System.out.println("Successfully connected to the database "
                        + prop.getProperty("database") + " as " + prop.getProperty("user"));
                con.setAutoCommit(false);
            }
        } catch (SQLException e) {
            System.err.println("Database connection failed");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    public static void setPrepareStatement() {
        try {
            stmtPost = con.prepareStatement("INSERT INTO public.posts (title,content,posting_time,posting_city,author_name) " +
                    "VALUES (?,?,?,?,?);");
            stmtAuthor = con.prepareStatement("INSERT INTO public.authors (author_id,author_registration_time,author_phone,author_name,password) VALUES (?,?,?,?,?) ON CONFLICT (author_name) DO NOTHING ;");
            stmtFollow = con.prepareStatement("INSERT INTO public.author_followed (author_name,followed_author_name)" + "VALUES (?,?) ON CONFLICT(author_name,followed_author_name) DO NOTHING;");
            stmtFavr = con.prepareStatement("INSERT INTO public.author_favorited (post_id,favorited_author_name)" + "VALUES (?,?) ON CONFLICT(post_id,favorited_author_name) DO NOTHING;");
            stmtShare = con.prepareStatement("INSERT INTO public.share_author (post_id,shared_author_name)" + "VALUES (?,?) ON CONFLICT (post_id,shared_author_name) DO NOTHING;");
            stmtLike = con.prepareStatement("INSERT INTO public.like_post (post_id,author_name)" + "VALUES (?,?) ON CONFLICT (post_id,author_name) DO NOTHING;");
            stmtCate = con.prepareStatement("INSERT INTO public.category_post(post_id,category)" + "VALUES (?,?) ON CONFLICT (post_id,category) DO NOTHING;");
            stmtReply = con.prepareStatement("INSERT INTO public.replies (reply_id,postID,content,stars,author_name) VALUES (?,?,?,?,?) ON CONFLICT (postID,content,stars,author_name) DO NOTHING;");
            stmtSecondReply = con.prepareStatement("INSERT INTO public.second_replies (id,stars,author_name,content) VALUES (?,?,?,?) ON CONFLICT (stars,author_name,content) DO NOTHING;");
            stmtReToSecRe = con.prepareStatement("INSERT INTO public.replies_to_second_replies (reply_id,second_reply_id) VALUES (?,?) ON CONFLICT (reply_id,second_reply_id) DO NOTHING;");
        } catch (SQLException e) {
            System.err.println("Insert statement failed");
            System.err.println(e.getMessage());
            closeDB();
            System.exit(1);
        }
    }

    private static void closeDB() {
        if (con != null) {
            try {
                if (stmtPost != null) {
                    stmtPost.close();
                }
                con.close();
                con = null;
            } catch (Exception ignored) {
            }
        }
    }

    private static Properties loadDBUser() {
        Properties properties = new Properties();
        try {
            properties.load(new InputStreamReader(new FileInputStream("resources/dbUser.properties")));
            return properties;
        } catch (IOException e) {
            System.err.println("can not find db user file");
            throw new RuntimeException(e);
        }
    }



    public static void insertAuthor(String ID, String time, String phone, String name,String password) {
        try {
            stmtAuthor.setString(1, ID);
            stmtAuthor.setTimestamp(2, Timestamp.valueOf(time));
            stmtAuthor.setString(3, phone);
            stmtAuthor.setString(4, name);
            stmtAuthor.setString(5,password);
            stmtAuthor.addBatch();
            stmtAuthor.executeBatch();
            con.commit();
            cnt++;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }

    public static String GenerateAuthorId() {
        int length = 18;
        String digits = "0123456789";
        Random rand = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = rand.nextInt(digits.length());
            char randomChar = digits.charAt(index);
            sb.append(randomChar);
        }
        String randomString = sb.toString();
        return randomString;
    }


    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        Properties prop = loadDBUser();
        long start = 0;
        long end = 0;
        try {
            String jsonStrings = Files.readString(Path.of("resources/posts.json"));
            List<Post> posts = JSON.parseArray(jsonStrings, Post.class);
            String jsonString = Files.readString(Path.of("resources/replies.json"));
            List<Replies> replies = JSON.parseArray(jsonString, Replies.class);
            start = System.currentTimeMillis();
            openDB(prop);
            setPrepareStatement();
            boolean isLogin = false;
            String authorName = "";
            String authorId = "";
            String authorPhone = "";
            String author_registration_time = "";
            String content = "";
            int stars;
            String title = "";
            while (true) {
                String sql = "";
                String a = in.next(); //命令 ：reg author_name password
                if (!a.equalsIgnoreCase("quit")) {
                    String[] cmd = a.trim().split(" ");
                    switch (cmd[0].toLowerCase(Locale.ROOT)) {
                        case "register": { //reg author_name
                            //判断author是否已被注册
                            System.out.print("Please input your name: ");
                            String b = in.next();
                            sql = "SELECT *\n" +
                                    "from authors\n" +
                                    "where author_name = ?;";
                            PreparedStatement ps = con.prepareStatement(sql);
                            ps.setString(1, b);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                System.out.print("The name has already been registered");
                            } else {
                                System.out.print("Input your password: ");
                                String password = in.next();
                                System.out.print("confirm your password: ");
                                if(Objects.equals(password, in.next())){
                                    System.out.print("Input your phone number: ");
                                    authorPhone = in.next();
                                    authorName = b;
                                    authorId = GenerateAuthorId();
                                    author_registration_time = getCurrentTime();
                                    insertAuthor(authorId, author_registration_time, authorPhone, authorName,password);
                                    System.out.println("register is finished.Please login in.");
                                }
                            }
                            break;
                        }

                        case "like": {//like post_id
                            //判断此post_id是否存在
                            System.out.print("The post you like is: ");
                            int b = in.nextInt();
                            sql = "SELECT *\n" +
                                    "from posts\n" +
                                    "where id = ?;";
                            PreparedStatement ps = con.prepareStatement(sql);
                            ps.setInt(1, b);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                stmtLike.setInt(1, b);
                                stmtLike.setString(2, authorName);
                                stmtLike.addBatch();
                                stmtAuthor.executeBatch();
                                stmtPost.executeBatch();
                                con.commit();
                            } else {
                                System.out.print("This post doesn't exist");
                            }
                            break;
                        }

                        case "favorite": {//favorite post_id
                            //判断此post_id是否存在
                            System.out.print("The post you favorite is: ");
                            int b = in.nextInt();
                            sql = "SELECT *\n" +
                                    "from posts\n" +
                                    "where id = ?;";
                            PreparedStatement ps = con.prepareStatement(sql);
                            ps.setInt(1, b);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                stmtFavr.setInt(1, b);
                                stmtFavr.setString(2, authorName);
                                stmtFavr.addBatch();
                                stmtAuthor.executeBatch();
                                stmtPost.executeBatch();
                                con.commit();
                            } else {
                                System.out.print("This post doesn't exist");
                            }
                            break;
                        }

                        case "share": {//share post_id
                            //判断此post_id是否存在
                            System.out.print("The post you share is: ");
                            int b = in.nextInt();
                            sql = "SELECT *\n" +
                                    "from posts\n" +
                                    "where id = ?;";
                            PreparedStatement ps = con.prepareStatement(sql);
                            ps.setInt(1, b);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                stmtShare.setInt(1, b);
                                stmtShare.setString(2, authorName);
                                stmtShare.addBatch();
                                stmtAuthor.executeBatch();
                                stmtPost.executeBatch();
                                con.commit();
                            } else {
                                System.out.print("This post doesn't exist");
                            }
                            break;
                        }

                        case "reply": {//reply post_id content
                            System.out.print("The post you reply is: ");
                            int b = in.nextInt();
                            sql = "SELECT *\n" +
                                    "from posts\n" +
                                    "where id = ?;";
                            PreparedStatement ps = con.prepareStatement(sql);
                            ps.setInt(1, b);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                stmtReply.setInt(1, b);
                                System.out.print("content: ");
                                content = in.next();
                                stmtReply.setString(2, content);
                                stmtReply.setInt(3, 0);
                                stmtReply.setString(4, authorName);
                                stmtReply.addBatch();
                                stmtAuthor.executeBatch();
                                con.commit();
                            } else {
                                System.out.print("This post doesn't exist");
                            }
                            break;
                        }

                        case "secondreply": {//reply reply_id content
                            System.out.print("The reply you want to reply is: ");
                            int b = in.nextInt();
                            sql = "SELECT *\n" +
                                    "from replies\n" +
                                    "where reply_id = ?;";
                            PreparedStatement ps = con.prepareStatement(sql);
                            ps.setInt(1, b);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                stmtSecondReply.setInt(1, 0);
                                stmtSecondReply.setString(2, authorName);
                                System.out.print("content: ");
                                content = in.next();
                                stmtSecondReply.setString(3, content);
                                stmtSecondReply.addBatch();
                                stmtAuthor.executeBatch();
                                String sql1 = "SELECT *\n" +
                                        "from second_replies\n" +
                                        "where content = ?;";
                                PreparedStatement ps1 = con.prepareStatement(sql1);
                                ps1.setString(1, content);
                                ResultSet rs1 = ps.executeQuery();
                                rs1.next();
                                stmtReToSecRe.setInt(1, b);
                                stmtReToSecRe.setInt(2, rs1.getInt("id"));
                                stmtSecondReply.executeBatch();
                                stmtReToSecRe.executeBatch();
                                con.commit();
                            } else {
                                System.out.print("This reply doesn't exist");
                            }
                            break;
                        }
                        case "post": {//post content
                            if (isLogin) {
                                System.out.print("Your post's title is: ");
                                title = in.next();
                                stmtPost.setString(1, title);
                                System.out.print("content: ");
                                content = in.next();
                                stmtPost.setString(2, content);
                                stmtPost.setString(3, getCurrentTime());
                                stmtPost.setString(4, "Shenzhen");
                                stmtPost.setString(5, authorName);
                            } else {
                                System.out.print("You have not logged in yet");
                            }
                            break;
                        }

                        case "checklist": {
                            //checklist follow or like or.....
                            System.out.print("Please input what list you want to search:");
                            String type = in.next();
                            switch (type.toLowerCase(Locale.ROOT)) {
                                case "follow": {
                                    sql = "select *\n" +
                                            "from author_followed\n" +
                                            "where author_name = ?;";
                                    PreparedStatement ps = con.prepareStatement(sql);
                                    ps.setString(1, authorName);
                                    ResultSet rs = ps.executeQuery();
                                    if (rs.next()) {
                                        System.out.println("author_name" + "     " + "author_id");
                                        System.out.println(rs.getString("author_name") + "     " + rs.getString("author_id"));
                                        while (rs.next()) {
                                            System.out.println(rs.getString("author_name") + "     " + rs.getString("author_id"));
                                        }
                                    } else {
                                        System.out.println("you don't follow any other authors");
                                    }
                                    break;
                                }

                                case "like": {
                                    sql = "select *\n" +
                                            "from like_post\n" +
                                            "where author_name = ?;";
                                    PreparedStatement ps = con.prepareStatement(sql);
                                    ps.setString(1, authorName);
                                    ResultSet rs = ps.executeQuery();
                                    if (rs.next()) {
                                        System.out.println("post_id");
                                        System.out.println(rs.getInt("post_id"));
                                        while (rs.next()) {
                                            System.out.println(rs.getInt("post_id"));
                                        }
                                    } else {
                                        System.out.println("you don't like any posts");
                                    }
                                    break;
                                }

                                case "favourite": {
                                    sql = "select *\n" +
                                            "from author_favorited\n" +
                                            "where favorited_author_name = ?;";
                                    PreparedStatement ps = con.prepareStatement(sql);
                                    ps.setString(1, authorName);
                                    ResultSet rs = ps.executeQuery();
                                    if (rs.next()) {
                                        System.out.println("post_id");
                                        System.out.println(rs.getInt("post_id"));
                                        while (rs.next()) {
                                            System.out.println(rs.getInt("post_id"));
                                        }
                                    } else {
                                        System.out.println("you don't like any posts");
                                    }
                                    break;
                                }

                            }
                            break;
                        }

                        case "follow": {
                            //follow author_name
                            //判断author是否存在
                            System.out.print("Please input the author:");
                            String followed_author = in.next();
                            sql = "SELECT *\n" +
                                    "from authors\n" +
                                    "where author_name = ?;";
                            PreparedStatement ps = con.prepareStatement(sql);
                            ps.setString(1, followed_author);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                stmtFollow.setString(1, authorName);
                                stmtFollow.setString(2, cmd[1]);
                                stmtFollow.addBatch();
                                stmtAuthor.executeBatch();
                            } else {
                                System.out.println("this author doesn't exist");
                            }
                            break;
                        }

                        case "login": {
                            //login author_name
                            System.out.print("author:");
                            authorName = in.next();
                            sql = "SELECT *\n" +
                                    "from authors\n" +
                                    "where author_name = ?;";
                            PreparedStatement ps = con.prepareStatement(sql);
                            ps.setString(1, authorName);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                String password = rs.getString("password");
                                System.out.print("Password:");
                                if (Objects.equals(in.next(), password)) {
                                    isLogin = true;

                                    System.out.println("login in successfully");
                                }else{
                                    System.out.println("your password is wrong");
                                    System.out.println(password);
                                }
                            } else {
                                System.out.println("author isn't existing.Please register first");
                            }
                            break;
                        }

                        case "search": {
                            //search for post / author /
                            System.out.println("if you don't want to find this ");


                        }
                    }
                } else {
                    isLogin = false;
                    authorName = "";
                    authorId = "";
                    authorPhone = "";
                    author_registration_time = "";

                }

            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        closeDB();
//        System.out.println(cnt + " records successfully loaded");
//        System.out.println(end - start);
//        System.out.println("Loading speed : " + (cnt * 1000L) / (end - start) + " records/s");
    }


}
