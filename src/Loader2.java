import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.sql.*;

import com.alibaba.fastjson.JSON;

import java.util.concurrent.ThreadLocalRandom;

public class Loader2 {
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
            stmtAuthor = con.prepareStatement("INSERT INTO public.authors (author_id,author_registration_time,author_phone,author_name) VALUES (?,?,?,?) ON CONFLICT (author_name) DO NOTHING ;");
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

    private static void loadAuthor(Post a) {
        if (con != null) {
            try {
                stmtAuthor.setString(1, a.getAuthorID());
                stmtAuthor.setString(2, a.getAuthorRegistrationTime());
                stmtAuthor.setString(3, a.getAuthorPhone());
                stmtAuthor.setString(4, a.getAuthor());
                stmtAuthor.addBatch();
                cnt++;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void loadPost(Post a) {
        if (con != null) {
            try {
                stmtPost.setInt(1, a.getPostID());
                stmtPost.setString(2, a.getTitle());
                stmtPost.setString(3, a.getContent());
                stmtPost.setString(4, a.getPostingTime());
                stmtPost.setString(5, a.getPostingCity());
                stmtPost.setString(6, a.getAuthor());
                PostTimeMap.put(a.getPostID(), a.getPostingTime());
                cnt++;
                stmtPost.addBatch();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private static void loadPostData(Post a) {
        if (con != null) {
            try {
                for (int i = 0; i < a.getAuthorFollowedBy().size(); i++) {
                    String regtime = GenerateTime(a.getPostingTime());
                    String id = GenerateAuthorId();
                    String phone = GeneratePhone();
                    insertAuthor(id, regtime, phone, a.getAuthorFollowedBy().get(i));
                    stmtFollow.setString(1, a.getAuthor());
                    stmtFollow.setString(2, a.getAuthorFollowedBy().get(i));
                    stmtFollow.addBatch();
                    cnt++;
                }

                for (int i = 0; i < a.getAuthorFavorite().size(); i++) {

                    String regtime = GenerateTime(a.getPostingTime());
                    String id = GenerateAuthorId();
                    String phone = GeneratePhone();
                    insertAuthor(id, regtime, phone, a.getAuthorFavorite().get(i));

                    stmtFavr.setInt(1, a.getPostID());
                    stmtFavr.setString(2, a.getAuthorFavorite().get(i));
                    stmtFavr.addBatch();
                    cnt++;
                }
                for (int i = 0; i < a.getAuthorShared().size(); i++) {

                    String regtime = GenerateTime(a.getPostingTime());
                    String id = GenerateAuthorId();
                    String phone = GeneratePhone();
                    insertAuthor(id, regtime, phone, a.getAuthorShared().get(i));

                    stmtShare.setInt(1, a.getPostID());
                    stmtShare.setString(2, a.getAuthorShared().get(i));
                    stmtShare.addBatch();
                    cnt++;
                }
                for (int i = 0; i < a.getAuthorLiked().size(); i++) {

                    String regtime = GenerateTime(a.getPostingTime());
                    String id = GenerateAuthorId();
                    String phone = GeneratePhone();
                    insertAuthor(id, regtime, phone, a.getAuthorLiked().get(i));
                    stmtLike.setInt(1, a.getPostID());
                    stmtLike.setString(2, a.getAuthorLiked().get(i));
                    stmtLike.addBatch();
                    cnt++;
                }
                for (int i = 0; i < a.getCategory().size(); i++) {
                    stmtCate.setInt(1, a.getPostID());
                    stmtCate.setString(2, a.getCategory().get(i));
                    stmtCate.addBatch();
                    cnt++;
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static void insertAuthor(String ID, String time, String phone, String name) {
        try {
            stmtAuthor.setString(1, ID);
            stmtAuthor.setString(2, time);
            stmtAuthor.setString(3, phone);
            stmtAuthor.setString(4, name);
            stmtAuthor.addBatch();
            cnt++;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String GenerateTime(String time) {
        LocalDateTime dateTime = LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime randomDateTime = dateTime.minusSeconds(ThreadLocalRandom.current().nextLong(0, dateTime.toEpochSecond(ZoneOffset.UTC)));
        return randomDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static String GeneratePhone() {
        int length = 10;
        String digits = "0123456789";
        Random rand = new Random();
        StringBuilder sb = new StringBuilder(length + 1);
        sb.append(1);
        for (int i = 0; i < length; i++) {
            int index = rand.nextInt(digits.length());
            char randomChar = digits.charAt(index);
            sb.append(randomChar);
        }
        String randomString = sb.toString();
        return randomString;
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

    public static void loadReply(Replies r) {
        if (con != null) {
            try {
                stmtReply.setInt(1, replyId);
                stmtReply.setInt(2, r.getPostID());
                stmtReply.setString(3, r.getReplyContent());
                stmtReply.setInt(4, r.getReplyStars());
                String regtime1 = GenerateTime(PostTimeMap.get(r.getPostID()));
                String id1 = GenerateAuthorId();
                String phone1 = GeneratePhone();
                insertAuthor(id1, regtime1, phone1, r.getReplyAuthor());
                stmtReply.setString(5, r.getReplyAuthor());
                stmtReply.addBatch();
                replyId++;
                cnt++;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void loadSecondReplies(Replies r) {
        if (con != null) {
            try {
                stmtSecondReply.setInt(1, secondReplyId);
                stmtSecondReply.setInt(2, r.getSecondaryReplyStars());
                String regtime = GenerateTime(PostTimeMap.get(r.getPostID()));
                String id = GenerateAuthorId();
                String phone = GeneratePhone();
                insertAuthor(id, regtime, phone, r.getSecondaryReplyAuthor());
                stmtSecondReply.setString(3, r.getSecondaryReplyAuthor());
                stmtSecondReply.setString(4, r.getSecondaryReplyContent());
                stmtSecondReply.addBatch();
                cnt++;
                stmtReToSecRe.setInt(1, findReply(r.getReplyContent(), r.getReplyStars(), r.getReplyAuthor()));
                stmtReToSecRe.setInt(2, secondReplyId);
                stmtReToSecRe.addBatch();
                cnt++;
                secondReplyId++;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static int findReply(String content, int stars, String author_name) {
        String sql = "SELECT reply_id FROM replies WHERE content = ? and stars = ? and author_name = ?;";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, content);
            ps.setInt(2, stars);
            ps.setString(3, author_name);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt("reply_id");
        } catch (SQLException e) {
            System.out.println("查询失败");
            // 记得要关闭 ResultSet 和 PreparedStatement 对象
            throw new RuntimeException(e);
        }
    }

    public static String findPostTime(int postId) {
        String sql = "SELECT posting_time FROM posts WHERE ID = ?;";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getString("posting_time");
        } catch (SQLException e) {
            System.out.println("查询失败");
            // 记得要关闭 ResultSet 和 PreparedStatement 对象
            throw new RuntimeException(e);
        }
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
            /*
             * 用户权限
             *
             *
             *
             * */
            boolean isInputName = false;
            boolean isLogin = false;
            String authorName = "";
            String authorId = "";
            String authorPhone = "";
            String author_registration_time = "";
            String password = "";
            while (true) {
                String sql = "";
                String a = in.next(); //命令 ：reg author_name password
                while (a.equalsIgnoreCase("quit")) {
                    String[] cmd = a.trim().split(" ");
                    switch (cmd[0].toLowerCase(Locale.ROOT)) {
                        case "reg"://reg author_name

                            break;
                        case "like"://like post_id

                            break;
                        case "favorite"://favorite post_id
                            break;

                        case "reply"://reply post_id content
                            break;

                        case "post"://post content
                            break;

                        case "share": {
                            //share post_id


                            break;


                        }


                        case "checklist": {
                            //checklist follow or like or.....
                            String type = cmd[1];
                            switch (type) {
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
                            }

                            break;
                        }
                        case "follow": {
                            //follow author_name
                            //判断author是否存在
                            sql = "SELECT *\n" +
                                    "from authors\n" +
                                    "where author_name = ?;";
                            PreparedStatement ps = con.prepareStatement(sql);
                            ps.setString(1, cmd[1]);
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
                            authorName = cmd[1];
                            sql = "SELECT *\n" +
                                    "from authors\n" +
                                    "where author_name = ?;";
                            PreparedStatement ps = con.prepareStatement(sql);
                            ps.setString(1, authorName);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                password = rs.getString("password");
                                isInputName = true;
                            } else {
                                System.out.println("author isn't existing.Please register first");
                            }
                            break;
                        }

                        case "pw": {
                            //pw password
                            if (isInputName) {
                                if (Objects.equals(password, cmd[1])) {
                                    isLogin = true;
                                } else {
                                    System.out.println("password is wrong");
                                }
                            }
                            break;
                        }
                        case "search"://search for post / author /
                    }
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
