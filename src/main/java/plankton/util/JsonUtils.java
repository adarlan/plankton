package plankton.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {

    private JsonUtils() {
        super();
    }

    public static <T> T parse(String json, Class<T> class1) {
        try {
            return new ObjectMapper().readValue(json, class1);
        } catch (JsonProcessingException e) {
            throw new JsonUtilsException("Unable to parse JSON: " + json, e);
        }
    }
}
