import java.io.*;
import java.net.*;

public class EchoServer {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(8888);
            System.out.println("Echo Server started. Listening on port 8888...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // 创建读取客户端消息的输入流
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                // 创建向客户端发送消息的输出流
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

                String message;
                while ((message = reader.readLine()) != null) {
                    System.out.println("Received from client: " + message);
                    // 将接收到的消息原样发送回客户端
                    writer.println("Server: " + message);
                }

                System.out.println("Client disconnected.");
                // 关闭连接
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
