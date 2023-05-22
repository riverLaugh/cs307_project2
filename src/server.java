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
            stmtPost = con.prepareStatement("INSERT INTO public.posts (ID,title,content,posting_time,posting_city,author_name) " +
                    "VALUES (?,?,?,?,?,?);");
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


    public static void insertAuthor(String ID, String time, String phone, String name, String password) {
        try {
            stmtAuthor.setString(1, ID);
            stmtAuthor.setTimestamp(2, Timestamp.valueOf(time));
            stmtAuthor.setString(3, phone);
            stmtAuthor.setString(4, name);
            stmtAuthor.setString(5, password);
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
                String a = in.nextLine(); //命令 ：reg author_name password
                if (!a.equalsIgnoreCase("quit")) {
                    switch (a.toLowerCase(Locale.ROOT)) {
                        case "register": { //reg author_name
                            System.out.print("Please input your name: ");
                            //判断author是否已被注册
                            String b = in.nextLine();
                            sql = "SELECT *\n" +
                                    "from authors\n" +
                                    "where author_name = ?;";
                            PreparedStatement ps = con.prepareStatement(sql);
                            ps.setString(1, b);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                System.out.println("The name has already been registered");
                            } else {
                                System.out.print("Input your password: ");
                                String password = in.nextLine();
                                System.out.print("confirm your password: ");
                                if (Objects.equals(password, in.next())) {
                                    System.out.print("Input your phone number: ");
                                    authorPhone = in.next();
                                    authorName = b;
                                    authorId = GenerateAuthorId();
                                    author_registration_time = getCurrentTime();
                                    insertAuthor(authorId, author_registration_time, authorPhone, authorName, password);
                                    System.out.println("register is finished.Please login in.");
                                }
                            }
                            break;
                        }

                        case "like": {//like post_id
                            if (isLogin) {
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
                                    System.out.println("you like this post");
                                } else {
                                    System.out.print("This post doesn't exist");
                                }
                            } else {
                                System.out.print("You have not logged in yet");
                            }
                            break;
                        }

                        case "favorite": {//favorite post_id
                            if (isLogin) {
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
                                    System.out.println("you favorite this post.");
                                } else {
                                    System.out.print("This post doesn't exist.");
                                }
                            } else {
                                System.out.print("You have not logged in yet");
                            }
                            break;
                        }

                        case "share": {//share post_id
                            if (isLogin) {
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
                                    System.out.println("you have share this post");
                                } else {
                                    System.out.print("This post doesn't exist");
                                }
                            } else {
                                System.out.print("You have not logged in yet");
                            }
                            break;
                        }

                        case "reply": {//reply post_id content
                            if (isLogin) {
                                System.out.print("The post you reply is: ");
                                int b = in.nextInt();
                                in.nextLine();
                                sql = "SELECT *\n" +
                                        "from posts\n" +
                                        "where id = ?;";
                                PreparedStatement ps = con.prepareStatement(sql);
                                ps.setInt(1, b);
                                ResultSet rs = ps.executeQuery();
                                if (rs.next()) {
                                    String s = "SELECT * FROM replies ORDER BY reply_id DESC LIMIT 1";
                                    PreparedStatement p = con.prepareStatement(s);
                                    ResultSet r = p.executeQuery();
                                    if (r.next()) {
                                        stmtReply.setInt(1, r.getInt("reply_id") + 1);
                                        System.out.print("content: ");
                                        content = in.nextLine();
                                        stmtReply.setInt(2, b);
                                        stmtReply.setString(3, content);
                                        stmtReply.setInt(4, 0);
                                        stmtReply.setString(5, authorName);
                                        stmtReply.addBatch();
                                        System.out.println("reply successfully");
                                    } else {
                                        System.out.print("nothing");
                                    }
                                } else {
                                    System.out.print("This post doesn't exist");
                                }
                            } else {
                                System.out.print("You have not logged in yet");
                            }
                            break;
                        }

                        case "secondreply": {//reply reply_id content
                            if (isLogin) {
                                System.out.print("The reply you want to reply is: ");
                                int b = in.nextInt();
                                in.nextLine();
                                sql = "SELECT *\n" +
                                        "from replies\n" +
                                        "where reply_id = ?;";
                                PreparedStatement ps = con.prepareStatement(sql);
                                ps.setInt(1, b);
                                ResultSet rs = ps.executeQuery();
                                if (rs.next()) {
                                    String sql1 = "SELECT * FROM second_replies ORDER BY id DESC LIMIT 1";
                                    PreparedStatement ps1 = con.prepareStatement(sql1);
                                    ResultSet rs1 = ps1.executeQuery();
                                    if (rs1.next()) {
                                        stmtSecondReply.setInt(1, rs1.getInt("id") + 1);
                                        stmtSecondReply.setInt(2, 0);
                                        System.out.print("content: ");
                                        content = in.nextLine();
                                        stmtSecondReply.setString(3, authorName);
                                        stmtSecondReply.setString(4, content);
                                        stmtSecondReply.addBatch();
                                        stmtReToSecRe.setInt(1, b);
                                        stmtReToSecRe.setInt(2, rs1.getInt("id") + 1);
                                        stmtReToSecRe.addBatch();
                                    } else {
                                        System.out.print("con not find");
                                    }
                                    System.out.print("reply successfully");
                                } else {
                                    System.out.print("This reply doesn't exist");
                                }
                            } else {
                                System.out.print("You have not logged in yet");
                            }
                            break;
                        }

                        case "post": {//post content
                            if (isLogin) {
                                String sql1 = "SELECT * FROM posts ORDER BY ID DESC LIMIT 1";
                                PreparedStatement ps1 = con.prepareStatement(sql1);
                                ResultSet rs1 = ps1.executeQuery();
                                if (rs1.next()) {
                                    stmtPost.setInt(1, rs1.getInt("ID") + 1);
                                    System.out.print("Your post's title is: ");
                                    in.nextLine();
                                    title = in.nextLine();
                                    stmtPost.setString(2, title);
                                    System.out.print("content: ");
                                    content = in.nextLine();
                                    stmtPost.setString(3, content);
                                    stmtPost.setTimestamp(4, Timestamp.valueOf(getCurrentTime()));
                                    stmtPost.setString(5, "Shenzhen");
                                    stmtPost.setString(6, authorName);
                                    stmtPost.addBatch();
                                    System.out.println("post successfully");
                                }
                            } else {
                                System.out.print("You have not logged in yet");
                            }
                            break;
                        }

                        case "list": {
                            //checklist follow or like or.....
                            if (isLogin) {
                                System.out.print("Please input what list you want to search(follow/like/favorite/post/repied post):");
                                String type = in.nextLine();
                                switch (type.toLowerCase(Locale.ROOT)) {
                                    case "post": {
                                        sql = "SELECT * FROM posts where author_name = ?;";
                                        PreparedStatement ps = con.prepareStatement(sql);
                                        ps.setString(1, authorName);
                                        ResultSet rs = ps.executeQuery();
                                        printPost(rs);
                                        break;
                                    }

                                    case "replied post": {
                                        sql = "SELECT * from replies where author_name= ?;";
                                        PreparedStatement ps = con.prepareStatement(sql);
                                        ps.setString(1, authorName);
                                        ResultSet rs = ps.executeQuery();
                                        int count = 0;
                                        while (rs.next()) {
                                            String sql1 = "SELECT * FROM posts where id = ?;";
                                            PreparedStatement ps1 = con.prepareStatement(sql1);
                                            ps1.setInt(1,rs.getInt("postID"));
                                            ResultSet rs1 = ps1.executeQuery();
                                            printPost(rs1);
                                            count++;
//                                            System.out.println("PostID:" + rs.getInt("postID"));
                                            System.out.print("replyID:" + rs.getInt("reply_id"));
                                            System.out.println();
                                            System.out.println("reply author:" + rs.getString("author_name"));
                                            System.out.print("content:" + rs.getString("content"));
                                            System.out.println();
                                            System.out.print("number of stars:" + rs.getInt("stars"));
                                            System.out.println();
                                            System.out.println("---------------------------------------------------------");
                                            System.out.println("---------------------------------------------------------");
                                        }
                                        if (count == 0) {
                                            System.out.println("you don't have reply");
                                        }
                                        break;
                                    }

                                    case "follow": {
                                        sql = "select *\n" +
                                                "from author_followed\n" +
                                                "where author_name = ?;";
                                        PreparedStatement ps = con.prepareStatement(sql);
                                        ps.setString(1, authorName);
                                        ResultSet rs = ps.executeQuery();
                                        if (rs.next()) {
                                            System.out.println("author_name");
                                            System.out.println(rs.getString("followed_author_name"));
                                            while (rs.next()) {
                                                System.out.println(rs.getString("followed_author_name"));
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

                                    case "favorite": {
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

                                    default: {
                                        System.out.println("wrong list type");
                                    }

                                }
                            }else{
                                System.out.println("you haven't log in");
                            }
                            break;
                        }

                        case "follow": {
                            //follow author_name
                            //判断author是否存在
                            if (isLogin) {
                                System.out.print("Please input the author:");
                                String followed_author = in.nextLine();
                                if (Objects.equals(followed_author, "quit")) {
                                    System.out.println("you have quited during this command process");
                                    continue;
                                }
                                sql = "SELECT *\n" +
                                        "from authors\n" +
                                        "where author_name = ?;";
                                PreparedStatement ps = con.prepareStatement(sql);
                                ps.setString(1, followed_author);
                                ResultSet rs = ps.executeQuery();
                                if (rs.next()) {
                                    stmtFollow.setString(1, authorName);
                                    stmtFollow.setString(2, followed_author);
                                    stmtFollow.addBatch();
                                    System.out.println("follow successfully.");
                                } else {
                                    System.out.println("this author doesn't exist.");
                                }
                            } else {
                                System.out.println("not log in");
                            }
                            break;
                        }

                        case "login": {
                            //login author_name
                            if (!isLogin) {
                                System.out.print("author:");
                                authorName = in.nextLine();
                                sql = "SELECT *\n" +
                                        "from authors\n" +
                                        "where author_name = ?;";
                                PreparedStatement ps = con.prepareStatement(sql);
                                ps.setString(1, authorName);
                                ResultSet rs = ps.executeQuery();
                                if (rs.next()) {
                                    String password = rs.getString("password");
                                    System.out.print("Password:");
                                    if (Objects.equals(in.nextLine(), password)) {
                                        isLogin = true;
                                        System.out.println("login in successfully");
                                    } else {
                                        System.out.println("your password is wrong");
                                        System.out.println(password);
                                    }
                                } else {
                                    System.out.println("author isn't existing.Please register first");
                                }
                            } else {
                                System.out.println("you have login.Please log out first");
                            }
                            break;
                        }

                        case "undo follow": {
                            if (isLogin) {
                                System.out.print("author:");
                                String unAuthor = in.nextLine();
                                sql = "SELECT *\n" +
                                        "from authors\n" +
                                        "where author_name = ?;";
                                PreparedStatement ps = con.prepareStatement(sql);
                                ps.setString(1, unAuthor);
                                ResultSet rs = ps.executeQuery();
                                if (rs.next()) {
                                    String sql2 = "SELECT *\n" +
                                            "from author_followed\n" +
                                            "where followed_author_name = ? and author_name = ?;";
                                    PreparedStatement ps2 = con.prepareStatement(sql2);
                                    ps2.setString(1, unAuthor);
                                    ps2.setString(2, authorName);
                                    ResultSet rs2 = ps2.executeQuery();
                                    if (rs2.next()) {
                                        String sql1 = "DELETE\n" +
                                                "from author_followed\n" +
                                                "where followed_author_name = ? and author_name = ?;";
                                        PreparedStatement ps1 = con.prepareStatement(sql1);
                                        ps1.setString(1, unAuthor);
                                        ps1.setString(2, authorName);
                                        int isUndo = ps1.executeUpdate();
                                        if (isUndo > 0) {
                                            System.out.println("You successfully unfollowed this author");
                                        } else {
                                            System.out.println("something wrong");
                                        }
                                    } else {
                                        System.out.println("You haven't followed the author");
                                    }
                                } else {
                                    System.out.println("author isn't existing.");
                                }
                            } else {
                                System.out.println("You have not logged in yet");
                            }
                            break;
                        }

                        case "search": {
                            //search for post / author /
                            System.out.println("if you don't want to find this ");

                            break;
                        }

                        case "block": {
                            System.out.print("which author do you want to block");//还需要把前面的命令改了
                            String bcked = in.nextLine();
                            sql = "INSERT INTO public.blocklist(author_name, blocked_name) VALUES (?,?) ON CONFLICT (author_name,blocked_name) DO NOTHING ;";
                            PreparedStatement ps = con.prepareStatement(sql);
                            ps.setString(1, authorName);
                            ps.setString(2, bcked);
                            ps.executeQuery();
                            break;
                        }

                        case "help": {
                            System.out.println("command: ");
                            System.out.println("--register: register an author");
                            System.out.println("--login: login ");
                            System.out.println("--post: post something you want to post");
                            System.out.println("--like: like a post");
                            System.out.println("--favorite: favorite a post");
                            System.out.println("--share: share a post");
                            System.out.println("--reply: reply a post");
                            System.out.println("--second reply: reply a reply");
                            System.out.println("--list: check some lists about yourself");
                            System.out.println("--follow: follow an author");
                            break;
                        }

                        default: {
                            System.out.println("wrong command");
                        }
                    }
                    stmtLike.executeBatch();
                    stmtAuthor.executeBatch();
                    stmtPost.executeBatch();
                    stmtSecondReply.executeBatch();
                    stmtReToSecRe.executeBatch();
                    stmtFollow.executeBatch();
                    stmtFavr.executeBatch();
                    stmtReply.executeBatch();
                    stmtShare.executeBatch();
                    stmtCate.executeBatch();
                    con.commit();
                } else {
                    isLogin = false;
                    authorName = "";
                    authorId = "";
                    authorPhone = "";
                    author_registration_time = "";
                    System.out.println("you log out successfully");
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
    public static void printPost(ResultSet rs) throws SQLException {
        int count = 0;
        while (rs.next()) {
            count++;
            System.out.print("PostID:" + rs.getInt("ID"));
            System.out.println();
            System.out.print("title:" + rs.getString("title"));
            System.out.println();
            System.out.print("content:" + rs.getString("content"));
            System.out.println();
            System.out.print("categories:");
            //category
            String sql1 = "SELECT * from category_post where post_id = ?";
            PreparedStatement ps1 = con.prepareStatement(sql1);
            ps1.setInt(1, rs.getInt("ID"));
            ResultSet rs1 = ps1.executeQuery();
            while (rs1.next()) {
                System.out.print(rs1.getString("category") + "/");
            }
            System.out.println();
            //reply
            System.out.print("number of like:");
            String sql2 = "SELECT count(*) from like_post where post_id = ? ";
            PreparedStatement ps2 = con.prepareStatement(sql2);
            ps2.setInt(1, rs.getInt("ID"));
            ResultSet rs2 = ps2.executeQuery();
            rs2.next();
            System.out.print(rs2.getInt(1));
            //favourite
            System.out.println();
            System.out.print("number of favourite:");
            String sql3 = "SELECT count(*) from author_favorited where post_id = ? ";
            PreparedStatement ps3 = con.prepareStatement(sql3);
            ps3.setInt(1, rs.getInt("ID"));
            ResultSet rs3 = ps3.executeQuery();
            rs3.next();
            System.out.print(rs3.getInt(1));
            System.out.println();
            System.out.println("---------------------------------------------------------");
        }
        if (count == 0) {
            System.out.println("none");
        }
    }

}