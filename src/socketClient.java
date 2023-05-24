import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Objects;
import java.util.Scanner;

/**
 * 客户端.
 *
 * @author jialin.li
 * @date 2019-12-10 22:30
 */
public class socketClient {
    public static void main(String[] args) throws IOException {
        // 指定ip 端口，创建socket
        String host = "127.0.0.1";
        int port = 8099;
        Socket communicationSocket = new Socket(host, port);
        // 获取输入流,读取服务器返回信息
        InputStream inputStream = communicationSocket.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        OutputStream outputStream = communicationSocket.getOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
        String message;
        bufferedWriter.write("try\n");
        bufferedWriter.flush();
        message = bufferedReader.readLine();
        Scanner in = new Scanner(System.in);
        if (Objects.equals(message, "welcome")) {
            System.out.println("Welcome!You can input the command now!");
        }
        while (true) {
//            System.out.println(1);
            String cmd = in.nextLine();
            bufferedWriter.write(cmd+"\n");
            bufferedWriter.flush();
            message = bufferedReader.readLine();
            System.out.printf("%s", message);
        }
//            communicationSocket.shutdownInput();
//            // 获取输出流,写入数据
//            communicationSocket.shutdownOutput();
        // 关闭资源
//        bufferedWriter.close();
//        bufferedReader.close();
    }
}