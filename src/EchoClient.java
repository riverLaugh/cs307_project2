import java.io.*;
import java.net.*;

public class EchoClient {
    public static void main(String[] args) {
        try {
            Socket clientSocket = new Socket("localhost", 8888);
            System.out.println("Connected to Echo Server.");

            // 创建向服务器发送消息的输出流
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            // 创建读取服务器回声的输入流
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            BufferedReader userInputReader = new BufferedReader(new InputStreamReader(System.in));
            String userInput;
            while ((userInput = userInputReader.readLine()) != null) {
                // 将用户输入的消息发送到服务器
                writer.println(userInput);

                // 读取服务器返回的消息并打印
                String serverResponse = reader.readLine();
                System.out.println("Server response: " + serverResponse);
            }

            // 关闭连接
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
