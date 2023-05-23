import java.io.*;
import java.net.*;

public class EchoClient {
    public static void main(String[] args) {
        try {
            Socket clientSocket = new Socket("localhost", 8888);
            System.out.println("Connected to Echo Server.");

            // �����������������Ϣ�������
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            // ������ȡ������������������
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            BufferedReader userInputReader = new BufferedReader(new InputStreamReader(System.in));
            String userInput;
            while ((userInput = userInputReader.readLine()) != null) {
                // ���û��������Ϣ���͵�������
                writer.println(userInput);

                // ��ȡ���������ص���Ϣ����ӡ
                String serverResponse = reader.readLine();
                System.out.println("Server response: " + serverResponse);
            }

            // �ر�����
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
