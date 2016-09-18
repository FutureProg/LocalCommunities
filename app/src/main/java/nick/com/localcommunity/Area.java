package nick.com.localcommunity;

/**
 * Created by Nick on 16-07-02.
 */
public class Area {

    public long id;
    public String name;

    public long getId(){
        return id;
    }

    public String getName(){
        return name;
    }


    @Override
    public String toString(){
        return id + ": " + name;
    }

}
