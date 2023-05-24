import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * �ͻ���.
 *
 * @author jialin.li
 * @date 2019-12-10 22:30
 */
public class TcpClient {
    public static void main(String[] args) throws IOException {
        // ָ��ip �˿ڣ�����socket
        String host = "127.0.0.1";
        int port = 8099;
        Socket communicationSocket = new Socket(host, port);
        // ��ȡ�����,д������
        OutputStream outputStream = communicationSocket.getOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
        bufferedWriter.write("hello world\n");
        bufferedWriter.flush();
        bufferedWriter.write("hello hwx");
        bufferedWriter.flush();
        communicationSocket.shutdownOutput();
        // ��ȡ������,��ȡ������������Ϣ
        InputStream inputStream = communicationSocket.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String message;
        while ((message = bufferedReader.readLine()) != null) {
            System.out.printf("get message from server : %s", message);
        }
        communicationSocket.shutdownInput();
        // �ر���Դ
        bufferedWriter.close();
        bufferedReader.close();
    }
}