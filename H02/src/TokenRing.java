import java.io.IOException;
import java.net.*;
import java.util.Stack;

public class TokenRing {

    
    private static void loop(DatagramSocket socket, String ip, int port, boolean first){
        
        Stack<Token.Endpoint> candidates = new Stack<>();
        
        if (first) {
            candidates.push(new Token.Endpoint(ip, port));
        }
        
        while (true) {
            try {

                Token rc = Token.receive(socket);
                Thread.sleep(1000);
                
                if (rc.length() == 1) {
                    candidates.push(rc.pollFirst());
                    if (!first) {
                        continue;
                    }
                }
                first = false;
                while(!candidates.empty())
                {   
                    rc.append(candidates.pop());
                }

                System.out.println(toString(rc));

                send(socket, rc);
            }
            catch (IOException e) {
                System.out.println(e.getMessage());
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public static void send(DatagramSocket s, Token rc) throws IOException
    {
        while(true)
        {
            //only this Node left
            if(rc.length() == 1)
            {
                break;
            }

            Token.Endpoint next = rc.pollFirst();
            rc.append(next);
            rc.incrementSequence();
            rc.send(s, next);
            if(Token.receivedAck(s))
            {
                break;
            }
            else
            {
                rc.pollLast();
            }
        }
    }


    public static String toString(Token rc)
    {
        String temp = "Token: seq="+rc.getSequence() + ", members=" + rc.length() + "{";
        for (Token.Endpoint endpoint : rc.getRing()) {
            temp = temp + "(" + endpoint.ip()  + ", " + endpoint.port() + ")";
        }
        temp = temp + "}\n";
        
        return temp;
    }

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            socket.disconnect();
            int port = socket.getLocalPort();
            System.out.printf("UDP endpoint is (%s %d)\n", ip, port);
            if (args.length == 0) {
                loop(socket,ip,port,true);
            }
            else if (args.length == 2) {
                Token rc = new Token().append(ip,port);
                rc.send(socket,args[0],Integer.parseInt(args[1]));
                if(Token.receivedAck(socket))
                {
                    loop(socket,ip,port,false);
                }
                else
                {
                    System.out.println("Host unreachable");
                }
            }
            else {
                System.out.println("Usage: \"java TokenRing\" or \"java TokenRing <ip> <port>\"");
            }
        }
        catch (SocketException e) {
            System.out.println("Error creating socket: " + e.getMessage());
        }
        catch (UnknownHostException e) {
            System.out.println("Error while determining IP address: " + e.getMessage());
        }
        catch (IOException e) {
            System.out.println("IO error: " + e.getMessage());
            System.out.println(e.getStackTrace());
        }
    }
}