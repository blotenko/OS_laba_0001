package funcClients;

import sun.misc.Signal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Optional;
import java.util.function.Function;

public class GProcess {
    private static Function<Integer, Optional<Optional<Integer>>> function;
    private  static Integer value;
    private static String res;
    public static void main(String[] args) {
        initSignalHandler();
        function = IntOps::trialG;
        try (
                Socket socket = new Socket("localhost", 1009);
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream()))
        {
            value = (Integer) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }

        Optional<Optional<Integer>> packed_result = function.apply(value);

        if(!packed_result.isPresent()){
            res =  "ghard";
        } else if (!packed_result.get().isPresent()){
            res =  "gsoft";
        } else {
            res = String.valueOf(packed_result.get().get());
        }
        try (
                Socket socket = new Socket("localhost", 1009);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream()))
        {
            out.writeObject(res);
            out.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void initSignalHandler(){
        Signal.handle(new Signal("INT"), signal -> initSignalHandler());
    }
}
