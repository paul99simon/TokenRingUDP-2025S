import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Deque;

public class Token {

    private static final int max_buffer_size = 4096;
    private static final int time_out_ms = 2000; //2 Second Timeout
    private static final int no_time_out = 0; //2 Second Timeout

    public record Endpoint(String ip, int port) {}

    public Token append(String ip, int port) {
        ring.offer(new Endpoint(ip, port));
        return this;
    }

    public Token append(Endpoint endpoint) {
        ring.offer(endpoint);
        return this;
    }

    public boolean remove(Endpoint Endpoint)
    {
        return ring.remove(Endpoint);
    }

    public boolean remove(String ip, int port)
    {
        return ring.remove(new Endpoint(ip, port));
    }

    public Endpoint first() {
        return ring.peekFirst();
    }

    public Endpoint pollFirst() {
        return ring.pollFirst();
    }

    public Endpoint last()
    {
        return ring.peekLast();
    }

    public Endpoint pollLast()
    {
        return ring.pollLast();
    }

    public int length () {
        return ring.size();
    }

    private int sequence = 0;

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public void incrementSequence() {
        sequence++;
    }

    public void send (DatagramSocket s, String ip_address, int port ) throws IOException {
        String rc_json = toJSON();
        byte[] rc_json_bytes = rc_json.getBytes(StandardCharsets.UTF_8);
        InetAddress address = InetAddress.getByName(ip_address);
        DatagramPacket packet = new DatagramPacket(rc_json_bytes, rc_json_bytes.length, address, port);
        System.out.printf("Sending %s to %s:%d\n", rc_json, ip_address, port);
        s.send(packet);
    }

    public void send (DatagramSocket s, Endpoint endpoint) throws IOException {
        send(s, endpoint.ip(), endpoint.port());
    }

    public static Token receive(DatagramSocket s) throws IOException {
        byte[] buf = new byte[max_buffer_size];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        s.receive(packet);
        String rc_json = new String(packet.getData(),0,packet.getLength(), StandardCharsets.UTF_8);
        System.out.printf("Received %s from %s:%d\n", rc_json, packet.getAddress().getHostAddress(), packet.getPort());
        return fromJSON(rc_json);
    }

    public static void sendAck(DatagramSocket s, String ip, int port) throws IOException
    {

        byte[] buf = new byte[max_buffer_size];
        InetAddress address = InetAddress.getByName(ip);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        s.send(packet);
    }

    public static void sendAck(DatagramSocket s, Endpoint e) throws IOException
    {
        byte[] buf = new byte[max_buffer_size];
        InetAddress address = InetAddress.getByName(e.ip);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, e.port);
        s.send(packet);
    }

    public static boolean receivedAck(DatagramSocket s) throws IOException
    {
        byte[] buf = new byte[max_buffer_size];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        s.setSoTimeout(time_out_ms);
        try {
            s.receive(packet);
        }
        catch (SocketTimeoutException sto)
        {
            return false;
        }
        s.setSoTimeout(no_time_out);
        return true;
    }

    @JsonProperty
    private final Deque<Endpoint> ring = new LinkedList<>();

    public Deque<Endpoint> getRing() {
        return ring;
    }

    private static final ObjectMapper serializer = new ObjectMapper();

    public String toJSON() throws JsonProcessingException {
        return serializer.writeValueAsString(this);
    }

    public static Token fromJSON(String json) throws IOException {
        return serializer.readValue(json, Token.class);
    }
}
