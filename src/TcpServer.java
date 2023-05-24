import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * ������.
 *
 * @author jialin.li
 * @date 2019-12-10 19:45
 */
public class TcpServer {
    public static void main(String[] args) throws IOException {
        int port = 8099;
        ServerSocket connectionSocket = new ServerSocket(port);
        // �����˿ڣ�acceptΪ��������
        System.out.println("Get socket successfully, wait for request...");
        Socket communicationSocket = connectionSocket.accept();
        // ��ȡ����������ȡ����
        InputStream inputStream = communicationSocket.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String message;
        message = bufferedReader.readLine();
        System.out.println(message);
        System.out.printf("get message from client : %s",message);
//        while ((message = bufferedReader.readLine()) != null) {
//            System.out.printf("get message from client : %s",message);
//        }
        communicationSocket.shutdownInput();
        // ��ȡ�����,���ؽ��
        OutputStream outputStream = communicationSocket.getOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
        bufferedWriter.write("I got your message and the communication is over.");
        bufferedWriter.flush();
        communicationSocket.shutdownOutput();
        // �ر���Դ
        bufferedWriter.close();
        bufferedReader.close();
    }
}