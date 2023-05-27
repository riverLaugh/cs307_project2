import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class client {
    private static Socket socket;
    private static BufferedReader reader;
    private static PrintWriter writer;

    public static void main(String[] args) {
        try {
            // 连接到服务端
            socket = new Socket("localhost", 12345); // 假设服务端监听在localhost的12345端口

            // 初始化输入输出流
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            // 读取用户输入并发送给服务端
            BufferedReader userInputReader = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                System.out.print("Enter command: ");
                String command = userInputReader.readLine();

                // 发送命令给服务端
                writer.println(command);
                writer.flush(); // 确保消息立即发送

                // 读取并打印服务端的响应
                String response;
                while ((response = reader.readLine()) != null) {
                    if (response.equalsIgnoreCase("quit")) {
                        break;
                    }
                    System.out.println(response);
                }
                // 判断退出条件
                if (command.equalsIgnoreCase("quit")) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭连接和流对象
            try {
                if (reader != null) {
                    reader.close();
                }
                if (writer != null) {
                    writer.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}