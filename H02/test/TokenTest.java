import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenTest {

    @Test
    void testMessage() throws IOException {
        Token m = new Token().append("ip0", 0).append("ip1", 1);
        String json_1 = m.toJSON();
        Token.Endpoint ipr = m.pollFirst();
        m.append(ipr.ip(), ipr.port());
        ipr = m.pollFirst();
        m.append(ipr.ip(), ipr.port());
        String json_2 = m.toJSON();
        assertEquals(json_1, json_2);
    }

    @Test
    void testRemove() throws JsonProcessingException
    {
        Token.Endpoint ep = new Token.Endpoint("ip0", 0);
        Token m = new Token().append(ep).append("ip1", 1);
        System.err.println(m.toJSON());

        m.remove(ep);
        assertTrue(m.getRing().size() == 1);
        System.err.println(m.toJSON());
        
        m.remove("ip1", 1);
        assertTrue(m.getRing().size() == 0);
        System.err.println(m.toJSON());
    
    }
}