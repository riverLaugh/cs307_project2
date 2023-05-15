import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.sql.*;
import java.util.Random;

import com.alibaba.fastjson.JSON;

import java.util.concurrent.ThreadLocalRandom;

public class insert {
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
    static HashMap<Integer,String> PostTimeMap = new HashMap<>();
    private static void openDB(Properties prop) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (Exception e) {
            System.err.println("Cannot find the Postgres driver. Check CLASSPATH.");
            System.exit(1);
        }
        String url = "jdbc:postgresql://" + prop.getProperty("host") + "/" + prop.getProperty("database")+"?characterEncoding=UTF-8";
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
            stmtAuthor = con.prepareStatement("INSERT INTO public.authors (author_id,author_registration_time,author_phone,author_name,password) VALUES (?,?,?,?,'111') ON CONFLICT (author_name) DO NOTHING ;");
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
                stmtAuthor.setTimestamp(2,Timestamp.valueOf(a.getAuthorRegistrationTime()));
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
                stmtPost.setTimestamp(4, Timestamp.valueOf(a.getPostingTime()));
                stmtPost.setString(5, a.getPostingCity());
                stmtPost.setString(6, a.getAuthor());
                PostTimeMap.put(a.getPostID(),a.getPostingTime());
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
            stmtAuthor.setTimestamp(2, Timestamp.valueOf(time));
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

//    public static String GenerateAuthorId(String timestampString) {
//    }



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

    public static void loadReply(Replies r){
        if(con!=null){
            try{
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
            }catch (SQLException e){
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
                stmtReToSecRe.setInt(1, findReply(r.getReplyContent(),r.getReplyStars(),r.getReplyAuthor()));
                stmtReToSecRe.setInt(2, secondReplyId);
                stmtReToSecRe.addBatch();
                cnt++;
                secondReplyId++;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static int findReply(String content,int stars,String author_name){
        String sql = "SELECT reply_id FROM replies WHERE content = ? and stars = ? and author_name = ?;";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1,content );
            ps.setInt(2,stars);
            ps.setString(3,author_name);
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
            for (int i = 0; i < posts.size(); i++) {
                Post a = posts.get(i);
                loadPost(a);
                loadAuthor(a);
            }
            stmtAuthor.executeBatch();
            stmtPost.executeBatch();
            for (int i = 0; i < posts.size(); i++) {
                Post a = posts.get(i);
                loadPostData(a);
            }
            stmtAuthor.executeBatch();
            stmtFollow.executeBatch();
            stmtFavr.executeBatch();
            stmtShare.executeBatch();
            stmtLike.executeBatch();
            stmtCate.executeBatch();
            con.commit();
            for (int i = 0; i < replies.size(); i++) {
                Replies r = replies.get(i);
                loadReply(r);
            }
            stmtAuthor.executeBatch();
            stmtReply.executeBatch();
            for (int i = 0; i < replies.size(); i++) {
                Replies r = replies.get(i);
                loadSecondReplies(r);
            }
            stmtAuthor.executeBatch();
            stmtSecondReply.executeBatch();
            stmtReToSecRe.executeBatch();
            end = System.currentTimeMillis();
            con.commit();
            con.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        closeDB();
//        System.out.println(cnt + " records successfully loaded");
        System.out.println(end - start + "ms");
//        System.out.println("Loading speed : " + (cnt * 1000L) / (end - start) + " records/s");
    }


}
