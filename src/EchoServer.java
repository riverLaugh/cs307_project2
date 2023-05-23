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

                // ������ȡ�ͻ�����Ϣ��������
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                // ������ͻ��˷�����Ϣ�������
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

                String message;
                while ((message = reader.readLine()) != null) {
                    System.out.println("Received from client: " + message);
                    // �����յ�����Ϣԭ�����ͻؿͻ���
                    writer.println("Server: " + message);
                }

                System.out.println("Client disconnected.");
                // �ر�����
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
