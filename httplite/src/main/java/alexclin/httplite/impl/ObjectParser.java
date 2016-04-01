package alexclin.httplite.impl;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;

import alexclin.httplite.MediaType;
import alexclin.httplite.Response;
import alexclin.httplite.exception.CanceledException;
import alexclin.httplite.exception.DecodeException;
import alexclin.httplite.exception.HttpException;
import alexclin.httplite.exception.ParserException;
import alexclin.httplite.listener.ResponseParser;
import alexclin.httplite.util.Util;

/**
 * ObjectParser
 *
 * @author alexclin  16/4/1 22:25
 */
public class ObjectParser {
    public interface Cancelable{
        boolean isCanceled();
    }

    private enum ParseType{
        RAW,Bitmap,String,Object
    }
    private Collection<ResponseParser> parsers;

    public ObjectParser(Collection<ResponseParser> parsers) {
        this.parsers = parsers;
    }

    public <T> T parseObject(Response response, Type type) throws Exception{
        return parseObject(response,type,null);
    }

    @SuppressWarnings("unchecked")
    public <T> T parseObject(Response response, Type type,Cancelable cancelable) throws Exception{
        ParseType parseType = handleType(type);
        int code = response.code();
        if(type==Boolean.class){
            return (T)Boolean.valueOf(code >= 200 && code < 300);
        }else if(type==Integer.class){
            return (T)Integer.valueOf(code);
        }else {
            if(!isSuccess(response)){
                throw responseToException(response);
            }
            switch (parseType){
                case RAW:
                    return (T)response;
                case Bitmap:
                    return (T)decodeBitmap(response);
                case String:
                    return (T)decodeToString(response);
                case Object:
                    if(parsers.isEmpty()){
                        throw new ParserException("No ResponseParser has set in HttpLite, failed with type:"+type);
                    }
                    for(ResponseParser parser: parsers){
                        if(cancelable!=null&&cancelable.isCanceled())
                            throw new CanceledException("Canceled during parse");
                        if(parser.isSupported(type)){
                            try {
                                return parser.parseResponse(response, type);
                            } catch (Exception e) {
                                throw new ParserException(e);
                            }
                        }
                    }
                default:
                    throw new ParserException("There is no ResponseParser in HttpLite support type:"+type);
            }
        }
    }

    private Bitmap decodeBitmap(Response response) throws Exception{
        try {
            return BitmapFactory.decodeStream(response.body().stream());
        } catch (IOException e) {
            throw new DecodeException("Decode Bitmap error",e);
        }
    }

    private ParseType handleType(Type type) {
        if(type==Response.class){
            return ParseType.RAW;
        }else if(type== Bitmap.class){
            return ParseType.Bitmap;
        }else if(type == String.class){
            return ParseType.String;
        }
        return ParseType.Object;
    }

    public static boolean isSuccess(Response response) {
        int code = response.code();
        return code >= 200 && code < 300;
    }

    public static HttpException responseToException(Response response) throws HttpException {
        String message = response.message();
        if (TextUtils.isEmpty(message)) {
            try {
                message = decodeToString(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new HttpException(response.code(), message);
    }

    public static String decodeToString(Response response) throws IOException {
        MediaType mt = response.body().contentType();
        if (mt != null) {
            Charset cs = mt.charset(Util.UTF_8);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().stream(), cs == null ? Util.UTF_8 : cs));
            StringBuilder stringBuilder = new StringBuilder();
            String s;
            while ((s = reader.readLine()) != null) {
                stringBuilder.append(s);
            }
            Util.closeQuietly(reader);
            return stringBuilder.toString();
        }
        throw new RuntimeException("Not text response body,no Content-Type in response");
    }
}