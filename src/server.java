//import java.io.*;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.time.*;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//import java.sql.*;
//
//import com.alibaba.fastjson.JSON;
//import com.mchange.v2.c3p0.ComboPooledDataSource;
//
//import javax.sql.DataSource;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//
//public class server {
//    private static Socket clientSocket;
//    private static BufferedReader reader;
//    private static PrintWriter writer;
//    private static final int PORT = 12345;
//    private static final int THREAD_POOL_SIZE = 10;
//    private static ExecutorService threadPool;
//
//    private static final int BATCH_SIZE = 1000;
//    private static Connection con = null;
//    private ResultSet resultSet;
//    private static PreparedStatement stmtPost = null;
//    private static PreparedStatement stmtAuthor = null;
//    private static PreparedStatement stmtFollow = null;
//    private static PreparedStatement stmtFavr = null;
//    private static PreparedStatement stmtShare = null;
//    private static PreparedStatement stmtLike = null;
//    private static PreparedStatement stmtCate = null;
//    private static PreparedStatement stmtReply = null;
//    private static PreparedStatement stmtSecondReply = null;
//    private static PreparedStatement stmtReToSecRe = null;
//    private static PreparedStatement stmtSearch = null;
//    private static PreparedStatement stmtBlock = null;
//    static int cnt = 0;
//    static int replyId = 1;
//    static int secondReplyId = 1;
//    static HashMap<Integer, String> PostTimeMap = new HashMap<>();
//
////    private static void openDB(Properties prop) {
////        try {
////            Class.forName("org.postgresql.Driver");
////        } catch (Exception e) {
////            System.err.println("Cannot find the Postgres driver. Check CLASSPATH.");
////            System.exit(1);
////        }
////        String url = "jdbc:postgresql://" + prop.getProperty("host") + "/" + prop.getProperty("database");
////        try {
////            con = DriverManager.getConnection(url, prop);
////            if (con != null) {
////                System.out.println("Successfully connected to the database "
////                        + prop.getProperty("database") + " as " + prop.getProperty("user"));
////                con.setAutoCommit(false);
////            }
////        } catch (SQLException e) {
////            System.err.println("Database connection failed");
////            System.err.println(e.getMessage());
////            System.exit(1);
////        }
////    }
//
//    static DataSource dataSource = new ComboPooledDataSource("sbcdb");
//
//    public static Connection getConnection() throws SQLException {
//        return dataSource.getConnection();
//    }
//
//    public static void setPrepareStatement() throws SQLException {
//        try {
//            stmtPost = con.prepareStatement("INSERT INTO public.posts (ID,title,content,posting_time,posting_city,author_name) " +
//                    "VALUES (?,?,?,?,?,?);");
//            stmtAuthor = con.prepareStatement("INSERT INTO public.authors (author_id,author_registration_time,author_phone,author_name,password) VALUES (?,?,?,?,?) ON CONFLICT (author_name) DO NOTHING ;");
//            stmtFollow = con.prepareStatement("INSERT INTO public.author_followed (author_name,followed_author_name)" + "VALUES (?,?) ON CONFLICT(author_name,followed_author_name) DO NOTHING;");
//            stmtFavr = con.prepareStatement("INSERT INTO public.author_favorited (post_id,favorited_author_name)" + "VALUES (?,?) ON CONFLICT(post_id,favorited_author_name) DO NOTHING;");
//            stmtShare = con.prepareStatement("INSERT INTO public.share_author (post_id,shared_author_name)" + "VALUES (?,?) ON CONFLICT (post_id,shared_author_name) DO NOTHING;");
//            stmtLike = con.prepareStatement("INSERT INTO public.like_post (post_id,author_name)" + "VALUES (?,?) ON CONFLICT (post_id,author_name) DO NOTHING;");
//            stmtCate = con.prepareStatement("INSERT INTO public.category_post(post_id,category)" + "VALUES (?,?) ON CONFLICT (post_id,category) DO NOTHING;");
//            stmtReply = con.prepareStatement("INSERT INTO public.replies (reply_id,postID,content,stars,author_name) VALUES (?,?,?,?,?) ON CONFLICT (postID,content,stars,author_name) DO NOTHING;");
//            stmtSecondReply = con.prepareStatement("INSERT INTO public.second_replies (id,stars,author_name,content) VALUES (?,?,?,?) ON CONFLICT (stars,author_name,content) DO NOTHING;");
//            stmtReToSecRe = con.prepareStatement("INSERT INTO public.replies_to_second_replies (reply_id,second_reply_id) VALUES (?,?) ON CONFLICT (reply_id,second_reply_id) DO NOTHING;");
//            stmtSearch = con.prepareStatement("INSERT INTO public.search_record (post_id,author_name) VALUES (?,?) ON CONFLICT (post_id,author_name) DO NOTHING;");
//            stmtBlock = con.prepareStatement("INSERT INTO public.blocklist(author_name, blocked_name) VALUES (?,?) ON CONFLICT (author_name,blocked_name) DO NOTHING;");
//        } catch (SQLException e) {
//            System.err.println("Insert statement failed");
//            System.err.println(e.getMessage());
////            closeDB();
//            con.close();
//            System.exit(1);
//        }
//    }
//
////    private static void closeDB() {
////        if (con != null) {
////            try {
////                if (stmtPost != null) {
////                    stmtPost.close();
////                }
////                con.close();
////                con = null;
////            } catch (Exception ignored) {
////            }
////        }
////    }
//
//    private static Properties loadDBUser() {
//        Properties properties = new Properties();
//        try {
//            properties.load(new InputStreamReader(new FileInputStream("resources/dbUser.properties")));
//            return properties;
//        } catch (IOException e) {
//            System.err.println("can not find db user file");
//            throw new RuntimeException(e);
//        }
//    }
//
//    public static void insertAuthor(String ID, String time, String phone, String name, String password) {
//        try {
//            stmtAuthor.setString(1, ID);
//            stmtAuthor.setTimestamp(2, Timestamp.valueOf(time));
//            stmtAuthor.setString(3, phone);
//            stmtAuthor.setString(4, name);
//            stmtAuthor.setString(5, password);
//            stmtAuthor.addBatch();
//            stmtAuthor.executeBatch();
//            con.commit();
//            cnt++;
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public static String getCurrentTime() {
//        LocalDateTime now = LocalDateTime.now();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        return now.format(formatter);
//    }
//
//    public static String GenerateAuthorId() {
//        int length = 18;
//        String digits = "0123456789";
//        Random rand = new Random();
//        StringBuilder sb = new StringBuilder(length);
//        for (int i = 0; i < length; i++) {
//            int index = rand.nextInt(digits.length());
//            char randomChar = digits.charAt(index);
//            sb.append(randomChar);
//        }
//        String randomString = sb.toString();
//        return randomString;
//    }
//
//    public static void main(String[] args) throws IOException {
//        threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
//
//        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
//            System.out.println("Server started. Listening on port " + PORT);
//
//            while (true) {
//                Socket clientSocket = serverSocket.accept();
//                System.out.println("Accepted connection from client: " + clientSocket.getInetAddress());
//
//                // 提交任务给线程池处理
//                threadPool.execute(new ClientHandler(clientSocket));
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            threadPool.shutdown();
//        }
//    }
//
//    private static class ClientHandler implements Runnable {
//        private Socket clientSocket;
//        private BufferedReader reader;
//        private PrintWriter writer;
//
//        public ClientHandler(Socket clientSocket) {
//            this.clientSocket = clientSocket;
//            try {
//                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//                writer = new PrintWriter(clientSocket.getOutputStream(), true);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        public void run() {
//            Properties prop = loadDBUser();
//            try {
//                String jsonStrings = Files.readString(Path.of("resources/posts.json"));
//                List<Post> posts = JSON.parseArray(jsonStrings, Post.class);
//                String jsonString = Files.readString(Path.of("resources/replies.json"));
//                List<Replies> replies = JSON.parseArray(jsonString, Replies.class);
////                openDB(prop);
//                con = getConnection();
//                setPrepareStatement();
//                boolean isLogin = false;
//                String authorName = "";
//                String authorId = "";
//                String authorPhone = "";
//                String author_registration_time = "";
//                String content = "";
//                int stars;
//                String title = "";
//                while (true) {
//                    String userInput = reader.readLine();
//                    String serverResponse = "";
//                    String sql = "";
//                    //命令 ：reg author_name password
//                    if (!userInput.equalsIgnoreCase("quit")) {
//                        switch (userInput.toLowerCase(Locale.ROOT)) {
//                            case "register": { //reg author_name
//                                writer.println("Please input your name: ");
//                                writer.flush();
//                                writer.println("quit");
//                                writer.flush();
//                                //判断author是否已被注册
//                                String b = reader.readLine();
//                                sql = "SELECT *\n" +
//                                        "from authors\n" +
//                                        "where author_name = ?;";
//                                PreparedStatement ps = con.prepareStatement(sql);
//                                ps.setString(1, b);
//                                ResultSet rs = ps.executeQuery();
//                                if (rs.next()) {
//                                    writer.println("The name has already been registered");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                } else {
//                                    writer.println("Input your password: ");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                    String password = reader.readLine();
//                                    writer.println("confirm your password: ");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                    if (Objects.equals(password, reader.readLine())) {
//                                        writer.println("Input your phone number: ");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                        authorPhone = reader.readLine();
////                                        System.out.println(authorPhone);
//                                        authorName = b;
//                                        authorId = GenerateAuthorId();
//                                        author_registration_time = getCurrentTime();
//                                        insertAuthor(authorId, author_registration_time, authorPhone, authorName, password);
//                                        writer.println("register is finished.Please login in.");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                    }
//                                }
//                                break;
//                            }
//
//                            case "like": {//like post_id
//                                if (isLogin) {
//                                    //判断此post_id是否存在
//                                    writer.println("The post you like is: ");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                    int b = Integer.parseInt(reader.readLine());
//                                    sql = "SELECT *\n" +
//                                            "from posts\n" +
//                                            "where id = ?;";
//                                    PreparedStatement ps = con.prepareStatement(sql);
//                                    ps.setInt(1, b);
//                                    ResultSet rs = ps.executeQuery();
//                                    if (rs.next()) {
//                                        stmtLike.setInt(1, b);
//                                        stmtLike.setString(2, authorName);
//                                        stmtLike.addBatch();
//                                        writer.println("you like this post");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                    } else {
//                                        writer.println("This post doesn't exist");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                    }
//                                } else {
//                                    writer.println("You have not logged in yet");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                }
//                                break;
//                            }
//
//                            case "favorite": {//favorite post_id
//                                if (isLogin) {
//                                    //判断此post_id是否存在
//                                    writer.println("The post you favorite is: ");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                    int b = Integer.parseInt(reader.readLine());
//                                    sql = "SELECT *\n" +
//                                            "from posts\n" +
//                                            "where id = ?;";
//                                    PreparedStatement ps = con.prepareStatement(sql);
//                                    ps.setInt(1, b);
//                                    ResultSet rs = ps.executeQuery();
//                                    if (rs.next()) {
//                                        stmtFavr.setInt(1, b);
//                                        stmtFavr.setString(2, authorName);
//                                        stmtFavr.addBatch();
//                                        writer.println("you favorite this post.");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                    } else {
//                                        writer.println("This post doesn't exist.");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                    }
//                                } else {
//                                    writer.println("You have not logged in yet");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                }
//                                break;
//                            }
//
//                            case "share": {//share post_id
//                                if (isLogin) {
//                                    //判断此post_id是否存在
//                                    writer.println("The post you share is: ");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                    int b = Integer.parseInt(reader.readLine());
//                                    sql = "SELECT *\n" +
//                                            "from posts\n" +
//                                            "where id = ?;";
//                                    PreparedStatement ps = con.prepareStatement(sql);
//                                    ps.setInt(1, b);
//                                    ResultSet rs = ps.executeQuery();
//                                    if (rs.next()) {
//                                        stmtShare.setInt(1, b);
//                                        stmtShare.setString(2, authorName);
//                                        stmtShare.addBatch();
//                                        writer.println("you have share this post");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                    } else {
//                                        writer.println("This post doesn't exist");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                    }
//                                } else {
//                                    writer.println("You have not logged in yet");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                }
//                                break;
//                            }
//
//                            case "reply": {//reply post_id content
//                                if (isLogin) {
//                                    writer.println("The post you reply is: ");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                    int b = Integer.parseInt(reader.readLine());
////                                    reader.readLine();
//                                    sql = "SELECT *\n" +
//                                            "from posts\n" +
//                                            "where id = ?;";
//                                    PreparedStatement ps = con.prepareStatement(sql);
//                                    ps.setInt(1, b);
//                                    ResultSet rs = ps.executeQuery();
//                                    if (rs.next()) {
//                                        String s = "SELECT * FROM replies ORDER BY reply_id DESC LIMIT 1";
//                                        PreparedStatement p = con.prepareStatement(s);
//                                        ResultSet r = p.executeQuery();
//                                        if (r.next()) {
//                                            stmtReply.setInt(1, r.getInt("reply_id") + 1);
//                                            writer.println("content: ");
//                                            writer.flush();
//                                            writer.println("quit");
//                                            writer.flush();
//                                            content = reader.readLine();
//                                            stmtReply.setInt(2, b);
//                                            stmtReply.setString(3, content);
//                                            stmtReply.setInt(4, 0);
//                                            stmtReply.setString(5, authorName);
//                                            stmtReply.addBatch();
//                                            writer.println("reply successfully");
//                                            writer.flush();
//                                            writer.println("quit");
//                                            writer.flush();
//                                        } else {
//                                            writer.println("nothing");
//                                            writer.flush();
//                                            writer.println("quit");
//                                            writer.flush();
//                                        }
//                                    } else {
//                                        writer.println("This post doesn't exist");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                    }
//                                } else {
//                                    writer.println("You have not logged in yet");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                }
//                                break;
//                            }
//
//                            case "secondreply": {//reply reply_id content
//                                if (isLogin) {
//                                    writer.println("The reply you want to reply is: ");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                    int b = Integer.parseInt(reader.readLine());
////                                    reader.readLine();
//                                    sql = "SELECT *\n" +
//                                            "from replies\n" +
//                                            "where reply_id = ?;";
//                                    PreparedStatement ps = con.prepareStatement(sql);
//                                    ps.setInt(1, b);
//                                    ResultSet rs = ps.executeQuery();
//                                    if (rs.next()) {
//                                        String sql1 = "SELECT * FROM second_replies ORDER BY id DESC LIMIT 1";
//                                        PreparedStatement ps1 = con.prepareStatement(sql1);
//                                        ResultSet rs1 = ps1.executeQuery();
//                                        if (rs1.next()) {
//                                            stmtSecondReply.setInt(1, rs1.getInt("id") + 1);
//                                            stmtSecondReply.setInt(2, 0);
//                                            writer.println("content: ");
//                                            writer.flush();
//                                            writer.println("quit");
//                                            writer.flush();
//                                            content = reader.readLine();
//                                            stmtSecondReply.setString(3, authorName);
//                                            stmtSecondReply.setString(4, content);
//                                            stmtSecondReply.addBatch();
//                                            stmtReToSecRe.setInt(1, b);
//                                            stmtReToSecRe.setInt(2, rs1.getInt("id") + 1);
//                                            stmtReToSecRe.addBatch();
//                                        } else {
//                                            writer.println("con not find");
//                                            writer.flush();
//                                            writer.println("quit");
//                                            writer.flush();
//                                        }
//                                        writer.println("reply successfully");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                    } else {
//                                        writer.println("This reply doesn't exist");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                    }
//                                } else {
//                                    writer.println("You have not logged in yet");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                }
//                                break;
//                            }
//
//                            case "post": {//post content
//                                if (isLogin) {
//                                    String sql1 = "SELECT * FROM posts ORDER BY ID DESC LIMIT 1";
//                                    PreparedStatement ps1 = con.prepareStatement(sql1);
//                                    ResultSet rs1 = ps1.executeQuery();
//                                    if (rs1.next()) {
//                                        int id = rs1.getInt("ID") + 1;
//                                        stmtPost.setInt(1, id);
//                                        writer.println("Your post's title is: ");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                        title = reader.readLine();
//                                        stmtPost.setString(2, title);
//                                        writer.println("content: ");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                        content = reader.readLine();
//                                        stmtPost.setString(3, content);
//                                        stmtPost.setTimestamp(4, Timestamp.valueOf(getCurrentTime()));
//                                        stmtPost.setString(5, "Shenzhen");
//                                        stmtPost.setString(6, authorName);
//                                        stmtPost.addBatch();
//                                        writer.println("post successfully");
//                                        writer.println("quit");
//                                        writer.flush();
//                                        writer.flush();
//                                        String category = reader.readLine();
//                                        stmtCate.setInt(1, id);
//                                        stmtCate.setString(2, category);
//                                    } else {
//                                        stmtPost.setInt(1, 1);
//                                        writer.println("Your post's title is: ");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                        title = reader.readLine();
//                                        stmtPost.setString(2, title);
//                                        writer.println("content: ");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                        content = reader.readLine();
//                                        stmtPost.setString(3, content);
//                                        stmtPost.setTimestamp(4, Timestamp.valueOf(getCurrentTime()));
//                                        stmtPost.setString(5, "Shenzhen");
//                                        stmtPost.setString(6, authorName);
//                                        stmtPost.addBatch();
//                                        writer.println("post successfully");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                        String category = reader.readLine();
//                                        stmtCate.setInt(1, 1);
//                                        stmtCate.setString(2, category);
//                                    }
//                                } else {
//                                    writer.println("You have not logged in yet");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                }
//                                break;
//                            }
//
//                            case "list": {
//                                //checklist follow or like or.....
//                                if (isLogin) {
//                                    writer.println("Please input what list you want to search(follow/like/favorite/post/replied post/share):");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                    String type = reader.readLine();
//                                    switch (type.toLowerCase(Locale.ROOT)) {
//
//                                        case "share": {
//                                            sql = "select *\n" +
//                                                    "from share_author\n" +
//                                                    "where shared_author_name = ?;";
//                                            PreparedStatement ps = con.prepareStatement(sql);
//                                            ps.setString(1, authorName);
//                                            ResultSet rs = ps.executeQuery();
//                                            if (rs.next()) {
//                                                writer.println("post_id");
//                                                writer.flush();
//                                                writer.println(rs.getInt("post_id"));
//                                                writer.flush();
//                                                while (rs.next()) {
//                                                    writer.println(rs.getInt("post_id"));
//                                                    writer.flush();
//                                                }
//                                            } else {
//                                                writer.println("you don't share any posts");
//                                                writer.flush();
//                                                writer.println("quit");
//                                                writer.flush();
//                                            }
//                                            writer.println("quit");
//                                            writer.flush();
//                                            break;
//                                        }
//
//                                        case "post": {
//                                            sql = "SELECT * FROM posts where author_name = ?;";
//                                            PreparedStatement ps = con.prepareStatement(sql);
//                                            ps.setString(1, authorName);
//                                            ResultSet rs = ps.executeQuery();
//
//                                            printPost(rs, writer);
//                                            break;
//                                        }
//
//                                        case "reply": {
//                                            sql = "select *\n" +
//                                                    "from replies\n" +
//                                                    "where reply_id in (\n" +
//                                                    "    select reply_id\n" +
//                                                    "    from replies_to_second_replies\n" +
//                                                    "    where second_reply_id in (select id from second_replies where author_name = ?))";
//                                            PreparedStatement ps = con.prepareStatement(sql);
//                                            ps.setString(1, authorName);
//                                            ResultSet rs = ps.executeQuery();
//                                            int count = 0;
//                                            while (rs.next()) {
//                                                count++;
//                                                writer.println("replyID:" + rs.getInt("reply_id"));
//                                                writer.flush();
//                                                writer.println("reply author:" + rs.getString("author_name"));
//                                                writer.flush();
//                                                writer.println("content:" + rs.getString("content"));
//                                                writer.flush();
//                                                writer.println("number of stars: " + rs.getInt("stars"));
//                                                writer.flush();
//                                                writer.println("---------------------------------------------------------");
//                                                writer.flush();
//                                            }
//                                            writer.println("quit");
//                                            writer.flush();
//                                            if (count == 0) {
//                                                writer.println("you dont reply any reply");
//                                                writer.flush();
//                                                writer.println("quit");
//                                                writer.flush();
//                                            }
//                                            break;
//                                        }
//
//                                        case "replied post": {
//                                            sql = "SELECT * from replies where author_name= ?;";
//                                            PreparedStatement ps = con.prepareStatement(sql);
//                                            ps.setString(1, authorName);
//                                            ResultSet rs = ps.executeQuery();
//                                            int count = 0;
//                                            while (rs.next()) {
//                                                String sql1 = "SELECT * FROM posts where id = ?;";
//                                                PreparedStatement ps1 = con.prepareStatement(sql1);
//                                                ps1.setInt(1, rs.getInt("postID"));
//                                                ResultSet rs1 = ps1.executeQuery();
//                                                printPost1(rs1, writer);
//                                                count++;
//                                            }
//                                            writer.println("quit");
//                                            writer.flush();
//                                            if (count == 0) {
//                                                writer.println("you don't have reply");
//                                                writer.flush();
//                                                writer.println("quit");
//                                                writer.flush();
//                                            }
//                                            break;
//                                        }
//
//                                        case "follow": {
//                                            sql = "select *\n" +
//                                                    "from author_followed\n" +
//                                                    "where author_name = ?;";
//                                            PreparedStatement ps = con.prepareStatement(sql);
//                                            ps.setString(1, authorName);
//                                            ResultSet rs = ps.executeQuery();
//                                            if (rs.next()) {
//                                                writer.println("author_name");
//                                                writer.flush();
//                                                writer.println(rs.getString("followed_author_name"));
//                                                writer.flush();
//                                                while (rs.next()) {
//                                                    writer.println(rs.getString("followed_author_name"));
//                                                    writer.flush();
//                                                }
//                                            } else {
//                                                writer.println("you don't follow any other authors");
//                                                writer.flush();
//                                                writer.println("quit");
//                                                writer.flush();
//                                            }
//                                            writer.println("quit");
//                                            writer.flush();
//                                            break;
//                                        }
//
//                                        case "like": {
//                                            sql = "select *\n" +
//                                                    "from like_post\n" +
//                                                    "where author_name = ?;";
//                                            PreparedStatement ps = con.prepareStatement(sql);
//                                            ps.setString(1, authorName);
//                                            ResultSet rs = ps.executeQuery();
//                                            if (rs.next()) {
//                                                writer.println("post_id");
//                                                writer.flush();
//                                                writer.println(rs.getInt("post_id"));
//                                                writer.flush();
//                                                while (rs.next()) {
//                                                    writer.println(rs.getInt("post_id"));
//                                                    writer.flush();
//                                                }
//                                            } else {
//                                                writer.println("you don't like any posts");
//                                                writer.flush();
//                                                writer.println("quit");
//                                                writer.flush();
//                                            }
//                                            writer.println("quit");
//                                            writer.flush();
//                                            break;
//                                        }
//
//                                        case "favorite": {
//                                            sql = "select *\n" +
//                                                    "from author_favorited\n" +
//                                                    "where favorited_author_name = ?;";
//                                            PreparedStatement ps = con.prepareStatement(sql);
//                                            ps.setString(1, authorName);
//                                            ResultSet rs = ps.executeQuery();
//                                            if (rs.next()) {
//                                                writer.println("post_id");
//                                                writer.flush();
//                                                writer.println(rs.getInt("post_id"));
//                                                writer.flush();
//                                                while (rs.next()) {
//                                                    writer.println(rs.getInt("post_id"));
//                                                    writer.flush();
//                                                }
//                                            } else {
//                                                writer.println("you don't favorite any posts");
//                                                writer.flush();
//                                                writer.println("quit");
//                                                writer.flush();
//                                            }
//                                            writer.println("quit");
//                                            writer.flush();
//                                            break;
//                                        }
//
//                                        default: {
//                                            writer.println("wrong list type");
//                                            writer.flush();
//                                            writer.println("quit");
//                                            writer.flush();
//                                        }
//
//                                    }
//                                } else {
//                                    writer.println("you haven't log in");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                }
//                                break;
//                            }
//
//                            case "follow": {
//                                //follow author_name
//                                //判断author是否存在
//                                if (isLogin) {
//                                    writer.println("Please input the author:");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                    String followed_author = reader.readLine();
//                                    if (Objects.equals(followed_author, "quit")) {
//                                        writer.println("you have quited during this command process");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                        continue;
//                                    }
//                                    sql = "SELECT *\n" +
//                                            "from authors\n" +
//                                            "where author_name = ?;";
//                                    PreparedStatement ps = con.prepareStatement(sql);
//                                    ps.setString(1, followed_author);
//                                    ResultSet rs = ps.executeQuery();
//                                    if (rs.next()) {
//                                        stmtFollow.setString(1, authorName);
//                                        stmtFollow.setString(2, followed_author);
//                                        stmtFollow.addBatch();
//                                        writer.println("follow successfully.");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                    } else {
//                                        writer.println("this author doesn't exist.");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                    }
//                                } else {
//                                    writer.println("not log in");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                }
//                                break;
//                            }
//
//                            case "login": {
//                                //login author_name
//                                if (!isLogin) {
//                                    writer.println("author:");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                    authorName = reader.readLine();
//                                    sql = "SELECT *\n" +
//                                            "from authors\n" +
//                                            "where author_name = ?;";
//                                    PreparedStatement ps = con.prepareStatement(sql);
//                                    ps.setString(1, authorName);
//                                    ResultSet rs = ps.executeQuery();
//                                    if (rs.next()) {
//                                        String password = rs.getString("password");
//                                        writer.println("Password:");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                        if (Objects.equals(reader.readLine(), password)) {
//                                            isLogin = true;
//                                            writer.println("login in successfully");
//                                            writer.flush();
//                                            writer.println("quit");
//                                            writer.flush();
//                                        } else {
//                                            writer.println("your password is wrong");
//                                            writer.flush();
//                                            writer.println("quit");
//                                            writer.flush();
//                                        }
//                                    } else {
//                                        writer.println("author isn't existing.Please register first");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                    }
//                                } else {
//                                    writer.println("you have login.Please log out first");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                }
//                                break;
//                            }
//
//                            case "undo follow": {
//                                if (isLogin) {
//                                    writer.println("author:");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                    String unAuthor = reader.readLine();
//                                    sql = "SELECT *\n" +
//                                            "from authors\n" +
//                                            "where author_name = ?;";
//                                    PreparedStatement ps = con.prepareStatement(sql);
//                                    ps.setString(1, unAuthor);
//                                    ResultSet rs = ps.executeQuery();
//                                    if (rs.next()) {
//                                        String sql2 = "SELECT *\n" +
//                                                "from author_followed\n" +
//                                                "where followed_author_name = ? and author_name = ?;";
//                                        PreparedStatement ps2 = con.prepareStatement(sql2);
//                                        ps2.setString(1, unAuthor);
//                                        ps2.setString(2, authorName);
//                                        ResultSet rs2 = ps2.executeQuery();
//                                        if (rs2.next()) {
//                                            String sql1 = "DELETE\n" +
//                                                    "from author_followed\n" +
//                                                    "where followed_author_name = ? and author_name = ?;";
//                                            PreparedStatement ps1 = con.prepareStatement(sql1);
//                                            ps1.setString(1, unAuthor);
//                                            ps1.setString(2, authorName);
//                                            int isUndo = ps1.executeUpdate();
//                                            if (isUndo > 0) {
//                                                writer.println("You successfully unfollowed this author");
//                                                writer.flush();
//                                                writer.println("quit");
//                                                writer.flush();
//                                            } else {
//                                                writer.println("something wrong");
//                                                writer.flush();
//                                                writer.println("quit");
//                                                writer.flush();
//                                            }
//                                        } else {
//                                            writer.println("You haven't followed the author");
//                                            writer.flush();
//                                            writer.println("quit");
//                                            writer.flush();
//                                        }
//                                    } else {
//                                        writer.println("author isn't existing.");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                    }
//                                } else {
//                                    writer.println("You have not logged in yet");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                }
//                                break;
//                            }
//
//                            case "search": {
//                                if (isLogin) {
//                                    writer.println("Enter year and month (YYYY-MM): ");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                    String yearMonthInput = reader.readLine();
//                                    writer.println("Enter ending year and month (YYYY-MM): ");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                    String endYearMonthInput = reader.readLine();
//                                    writer.println("Enter category: ");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                    String category = reader.readLine();
//                                    writer.println("Enter key: ");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                    String key = reader.readLine();
//
//                                    sql = "SELECT distinct p.* FROM posts p " +
//                                            "JOIN category_post cp ON p.id = cp.post_id " +
//                                            "WHERE 1=1";
//                                    if (!yearMonthInput.equalsIgnoreCase("all") && !endYearMonthInput.equalsIgnoreCase("all")) {
//                                        sql += " AND p.posting_time >= ? AND p.posting_time <= ?";
//                                    }
//                                    if (!category.isEmpty() && !category.equalsIgnoreCase("all")) {
//                                        sql += " AND cp.category = ?";
//                                    }
//                                    if (!key.isEmpty() && !key.equalsIgnoreCase("all")) {
//                                        sql += " AND (p.title LIKE ? OR p.content LIKE ?)";
//                                    }
//
//                                    PreparedStatement ps = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
//                                    int paramIndex = 1;
//                                    if (!yearMonthInput.equalsIgnoreCase("all") && !endYearMonthInput.equalsIgnoreCase("all")) {
//                                        String startDateTime = yearMonthInput + "-01 00:00:00";
//                                        String endDateTime = endYearMonthInput + "-31 23:59:59";
//                                        ps.setTimestamp(paramIndex++, Timestamp.valueOf(startDateTime));
//                                        ps.setTimestamp(paramIndex++, Timestamp.valueOf(endDateTime));
//                                    }
//                                    if (!category.isEmpty() && !category.equalsIgnoreCase("all")) {
//                                        ps.setString(paramIndex++, category);
//                                    }
//                                    if (!key.isEmpty() && !key.equalsIgnoreCase("all")) {
//                                        ps.setString(paramIndex++, "%" + key + "%");
//                                        ps.setString(paramIndex++, "%" + key + "%");
//                                    }
//
//                                    ResultSet rs = ps.executeQuery();
//                                    printPost(rs, writer);
//                                    rs.beforeFirst();
//                                    while (rs.next()) {
//                                        stmtSearch.setInt(1, rs.getInt("id"));
//                                        stmtSearch.setString(2, authorName);
//                                        stmtSearch.addBatch();
//                                    }
//                                } else {
//                                    writer.println("You have not logged in yet");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                }
//                                break;
//                            }
//
//                            case "block": {
//                                if (isLogin) {
//                                    writer.println("which author do you want to block: ");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                    String bcked = reader.readLine();
//                                    sql = "SELECT *\n" +
//                                            "from authors\n" +
//                                            "where author_name = ?;";
//                                    PreparedStatement ps = con.prepareStatement(sql);
//                                    ps.setString(1, bcked);
//                                    ResultSet rs = ps.executeQuery();//还需要把前面的命令改了
//                                    if (rs.next()) {
//                                        stmtBlock.setString(1, authorName);
//                                        stmtBlock.setString(2, bcked);
//                                        stmtBlock.addBatch();
//                                        writer.println("You have blocked this author.");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                    } else {
//                                        writer.println("this author doesn't exist.");
//                                        writer.flush();
//                                        writer.println("quit");
//                                        writer.flush();
//                                    }
//                                } else {
//                                    writer.println("You have not logged in yet");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                }
//                                break;
//                            }
//
//                            case "help": {
//                                writer.println("command: ");
//                                writer.flush();
//                                writer.println("--register: register an author");
//                                writer.flush();
//                                writer.println("--login: login ");
//                                writer.flush();
//                                writer.println("--post: post something you want to post");
//                                writer.flush();
//                                writer.println("--like: like a post");
//                                writer.flush();
//                                writer.println("--favorite: favorite a post");
//                                writer.flush();
//                                writer.println("--share: share a post");
//                                writer.flush();
//                                writer.println("--reply: reply a post");
//                                writer.flush();
//                                writer.println("--second reply: reply a reply");
//                                writer.flush();
//                                writer.println("--list: check some lists about yourself");
//                                writer.flush();
//                                writer.println("--follow: follow an author");
//                                writer.flush();
//                                writer.println("quit");
//                                writer.flush();
//                                break;
//                            }
//
//                            case "anonymous": {
//                                if (isLogin) {
//                                    writer.println("Please enter the type you want to talk (post/reply/secondreply): ");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                    String type = reader.readLine();
//                                    switch (type.toLowerCase(Locale.ROOT)) {
//
//                                        case "post": {
//                                            String sql1 = "SELECT * FROM posts ORDER BY ID DESC LIMIT 1";
//                                            PreparedStatement ps1 = con.prepareStatement(sql1);
//                                            ResultSet rs1 = ps1.executeQuery();
//                                            if (rs1.next()) {
//                                                int id = rs1.getInt("ID") + 1;
//                                                stmtPost.setInt(1, id);
//                                                writer.println("Your post's title is: ");
//                                                writer.flush();
//                                                writer.println("quit");
//                                                writer.flush();
//                                                title = reader.readLine();
//                                                stmtPost.setString(2, title);
//                                                writer.println("content: ");
//                                                writer.flush();
//                                                writer.println("quit");
//                                                writer.flush();
//                                                content = reader.readLine();
//                                                stmtPost.setString(3, content);
//                                                stmtPost.setTimestamp(4, Timestamp.valueOf(getCurrentTime()));
//                                                stmtPost.setString(5, "anonymous");
//                                                stmtPost.setString(6, "anonymous");
//                                                stmtPost.addBatch();
//                                                writer.println("post successfully");
//                                                writer.flush();
//                                                writer.println("quit");
//                                                writer.flush();
//                                            } else {
//                                                stmtPost.setInt(1, 1);
//                                                writer.println("Your post's title is: ");
//                                                writer.flush();
//                                                writer.println("quit");
//                                                writer.flush();
//                                                title = reader.readLine();
//                                                stmtPost.setString(2, title);
//                                                writer.println("content: ");
//                                                writer.flush();
//                                                writer.println("quit");
//                                                writer.flush();
//                                                content = reader.readLine();
//                                                stmtPost.setString(3, content);
//                                                stmtPost.setTimestamp(4, Timestamp.valueOf(getCurrentTime()));
//                                                stmtPost.setString(5, "anonymous");
//                                                stmtPost.setString(6, "anonymous");
//                                                stmtPost.addBatch();
//                                                writer.println("post successfully");
//                                                writer.flush();
//                                                writer.println("quit");
//                                                writer.flush();
//                                            }
//                                            break;
//                                        }
//
//                                        case "reply": {
//                                            writer.println("The post you reply is: ");
//                                            writer.flush();
//                                            writer.println("quit");
//                                            writer.flush();
//                                            int b = Integer.parseInt(reader.readLine());
////                                            reader.readLine();
//                                            sql = "SELECT *\n" +
//                                                    "from posts\n" +
//                                                    "where id = ?;";
//                                            PreparedStatement ps = con.prepareStatement(sql);
//                                            ps.setInt(1, b);
//                                            ResultSet rs = ps.executeQuery();
//                                            if (rs.next()) {
//                                                String s = "SELECT * FROM replies ORDER BY reply_id DESC LIMIT 1";
//                                                PreparedStatement p = con.prepareStatement(s);
//                                                ResultSet r = p.executeQuery();
//                                                if (r.next()) {
//                                                    stmtReply.setInt(1, r.getInt("reply_id") + 1);
//                                                    writer.println("content: ");
//                                                    writer.flush();
//                                                    writer.println("quit");
//                                                    writer.flush();
//                                                    content = reader.readLine();
//                                                    stmtReply.setInt(2, b);
//                                                    stmtReply.setString(3, content);
//                                                    stmtReply.setInt(4, 0);
//                                                    stmtReply.setString(5, "anonymous");
//                                                    stmtReply.addBatch();
//                                                    writer.println("reply successfully");
//                                                    writer.flush();
//                                                    writer.println("quit");
//                                                    writer.flush();
//                                                } else {
//                                                    writer.println("nothing");
//                                                    writer.flush();
//                                                    writer.println("quit");
//                                                    writer.flush();
//                                                }
//                                            } else {
//                                                writer.println("This post doesn't exist");
//                                                writer.flush();
//                                                writer.println("quit");
//                                                writer.flush();
//                                            }
//                                            break;
//                                        }
//
//                                        case "secondreply": {
//                                            writer.println("The reply you want to reply is: ");
//                                            writer.flush();
//                                            writer.println("quit");
//                                            writer.flush();
//                                            int b = Integer.parseInt(reader.readLine());
////                                            reader.readLine();
//                                            sql = "SELECT *\n" +
//                                                    "from replies\n" +
//                                                    "where reply_id = ?;";
//                                            PreparedStatement ps = con.prepareStatement(sql);
//                                            ps.setInt(1, b);
//                                            ResultSet rs = ps.executeQuery();
//                                            if (rs.next()) {
//                                                String sql1 = "SELECT * FROM second_replies ORDER BY id DESC LIMIT 1";
//                                                PreparedStatement ps1 = con.prepareStatement(sql1);
//                                                ResultSet rs1 = ps1.executeQuery();
//                                                if (rs1.next()) {
//                                                    stmtSecondReply.setInt(1, rs1.getInt("id") + 1);
//                                                    stmtSecondReply.setInt(2, 0);
//                                                    writer.println("content: ");
//                                                    writer.flush();
//                                                    writer.println("quit");
//                                                    writer.flush();
//                                                    content = reader.readLine();
//                                                    stmtSecondReply.setString(3, "anonymous");
//                                                    stmtSecondReply.setString(4, content);
//                                                    stmtSecondReply.addBatch();
//                                                    stmtReToSecRe.setInt(1, b);
//                                                    stmtReToSecRe.setInt(2, rs1.getInt("id") + 1);
//                                                    stmtReToSecRe.addBatch();
//                                                } else {
//                                                    writer.println("con not find");
//                                                    writer.flush();
//                                                    writer.println("quit");
//                                                    writer.flush();
//                                                }
//                                                writer.println("reply successfully");
//                                                writer.flush();
//                                                writer.println("quit");
//                                                writer.flush();
//                                            } else {
//                                                writer.println("This reply doesn't exist");
//                                                writer.flush();
//                                                writer.println("quit");
//                                                writer.flush();
//                                            }
//                                        }
//                                    }
//                                } else {
//                                    writer.println("You have not logged in yet");
//                                    writer.flush();
//                                    writer.println("quit");
//                                    writer.flush();
//                                }
//                                break;
//                            }
//
//                            case "hot": {
//                                sql = "SELECT post_id, COUNT(post_id) AS times\n" +
//                                        "FROM search_record\n" +
//                                        "GROUP BY post_id\n" +
//                                        "ORDER BY times DESC\n" +
//                                        "LIMIT 10;";
//                                PreparedStatement ps = con.prepareStatement(sql);
//                                ResultSet rs = ps.executeQuery();
//                                while (rs.next()) {
//                                    int postId = rs.getInt("post_id");
//                                    int times = rs.getInt("times");
//                                    writer.println("Post ID: " + postId + ", Times: " + times);
//                                    writer.flush();
//                                }
//                                writer.println("quit");
//                                writer.flush();
//                                break;
//                            }
//
//                            default: {
//                                writer.println("wrong command");
//                                writer.flush();
//                                writer.println("quit");
//                                writer.flush();
//                                break;
//                            }
//                        }
//                        stmtLike.executeBatch();
//                        stmtAuthor.executeBatch();
//                        stmtPost.executeBatch();
//                        stmtSecondReply.executeBatch();
//                        stmtReToSecRe.executeBatch();
//                        stmtFollow.executeBatch();
//                        stmtFavr.executeBatch();
//                        stmtReply.executeBatch();
//                        stmtShare.executeBatch();
//                        stmtCate.executeBatch();
//                        stmtSearch.executeBatch();
//                        stmtBlock.executeBatch();
//                    } else {
//                        isLogin = false;
//                        authorName = "";
//                        authorId = "";
//                        authorPhone = "";
//                        author_registration_time = "";
//                        writer.println("you log out successfully");
//                        writer.flush();
//                        writer.println("quit");
//                        writer.flush();
//                    }
//
//                }
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (SQLException e) {
//                e.printStackTrace();
//            } finally {
//                try {
//                    clientSocket.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//        }
//    }
//
//    public static void printPost(ResultSet rs, PrintWriter writer) throws SQLException {
//        int count = 0;
//        while (rs.next()) {
//            count++;
//            writer.println("PostID:" + rs.getInt("ID"));
//            writer.flush();
//            writer.println("title:" + rs.getString("title"));
//            writer.flush();
//            writer.println("content:" + rs.getString("content"));
//            writer.flush();
//            writer.println("categories:");
//            writer.flush();
//            //category
//            String sql1 = "SELECT * from category_post where post_id = ?";
//            PreparedStatement ps1 = con.prepareStatement(sql1);
//            ps1.setInt(1, rs.getInt("ID"));
//            ResultSet rs1 = ps1.executeQuery();
//            while (rs1.next()) {
//                writer.println(rs1.getString("category") + "/");
//                writer.flush();
//            }
//            //reply
//            writer.println("number of like:");
//            writer.flush();
//            String sql2 = "SELECT count(*) from like_post where post_id = ? ";
//            PreparedStatement ps2 = con.prepareStatement(sql2);
//            ps2.setInt(1, rs.getInt("ID"));
//            ResultSet rs2 = ps2.executeQuery();
//            rs2.next();
//            writer.println(rs2.getInt(1));
//            writer.flush();
//            //favourite
//            writer.println("number of favourite:");
//            writer.flush();
//            String sql3 = "SELECT count(*) from author_favorited where post_id = ? ";
//            PreparedStatement ps3 = con.prepareStatement(sql3);
//            ps3.setInt(1, rs.getInt("ID"));
//            ResultSet rs3 = ps3.executeQuery();
//            rs3.next();
//            writer.println(rs3.getInt(1));
//            writer.flush();
//            writer.println("---------------------------------------------------------");
//            writer.flush();
//
//        }
//        writer.println("quit");
//        writer.flush();
//        if (count == 0) {
//            writer.println("none");
//            writer.flush();
//            writer.println("quit");
//            writer.flush();
//        }
//    }
//
//    public static void printPost1(ResultSet rs, PrintWriter writer) throws SQLException {
//        int count = 0;
//        while (rs.next()) {
//            count++;
//            writer.println("PostID:" + rs.getInt("ID"));
//            writer.flush();
//            writer.println("title:" + rs.getString("title"));
//            writer.flush();
//            writer.println("content:" + rs.getString("content"));
//            writer.flush();
//            writer.println("categories:");
//            writer.flush();
//            //category
//            String sql1 = "SELECT * from category_post where post_id = ?";
//            PreparedStatement ps1 = con.prepareStatement(sql1);
//            ps1.setInt(1, rs.getInt("ID"));
//            ResultSet rs1 = ps1.executeQuery();
//            while (rs1.next()) {
//                writer.println(rs1.getString("category") + "/");
//                writer.flush();
//            }
//            //reply
//            writer.println("number of like:");
//            writer.flush();
//            String sql2 = "SELECT count(*) from like_post where post_id = ? ";
//            PreparedStatement ps2 = con.prepareStatement(sql2);
//            ps2.setInt(1, rs.getInt("ID"));
//            ResultSet rs2 = ps2.executeQuery();
//            rs2.next();
//            writer.println(rs2.getInt(1));
//            writer.flush();
//            //favourite
//            writer.println("number of favourite:");
//            writer.flush();
//            String sql3 = "SELECT count(*) from author_favorited where post_id = ? ";
//            PreparedStatement ps3 = con.prepareStatement(sql3);
//            ps3.setInt(1, rs.getInt("ID"));
//            ResultSet rs3 = ps3.executeQuery();
//            rs3.next();
//            writer.println(rs3.getInt(1));
//            writer.flush();
//            writer.println("---------------------------------------------------------");
//            writer.flush();
//
//        }
////        writer.println("quit");
////        writer.flush();
//        if (count == 0) {
//            writer.println("none");
//            writer.flush();
//            writer.println("quit");
//            writer.flush();
//        }
//    }
//
//}