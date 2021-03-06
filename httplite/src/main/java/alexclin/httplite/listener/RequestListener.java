package alexclin.httplite.listener;

import java.lang.reflect.Type;

import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;

/**
 * RequestListener
 *
 * @author alexclin 16/1/18 21:37
 */
public interface RequestListener {
    void onRequest(HttpLite lite,Request request, Type resultType);
}
