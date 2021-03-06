package alexclin.httplite;

import alexclin.httplite.listener.Callback;
import alexclin.httplite.util.Clazz;
import alexclin.httplite.util.Result;
import alexclin.httplite.util.Util;

/**
 * Call
 *
 * @author alexclin at 16/1/29 21:15
 */
public abstract class Call implements Handle{
    protected Request request;
    private Executable executable;

    public Call(Request request) {
        this.request = request;
    }

    public final <T> Handle async(Callback<T> callback){
        async(Util.type(Callback.class,callback)!=Response.class,callback);
        return this;
    }

    public abstract <T> Handle async(boolean callOnMain,Callback<T> callback);

    public final Response sync() throws Exception{
        return sync(new Clazz<Response>() {});
    }

    public abstract <T> T sync(Clazz<T> clazz) throws Exception;

    public abstract <T> Result<T> syncResult(Clazz<T> clazz);

    public Request request(){
        return request;
    }

    public void cancel(){
        if(executable!=null&&!executable().isCanceled()&&!executable().isExecuted())
            executable.cancel();
    }

    public boolean isCanceled(){
        return executable!=null&&executable.isCanceled();
    }

    public boolean isExecuted(){
        return executable!=null&&executable.isExecuted();
    }

    protected void setExecutable(Executable executable){
        this.executable = executable;
    }

    protected Executable executable(){
        return executable;
    }

    interface CallFactory {
        Call newCall(Request request);
    }
}
