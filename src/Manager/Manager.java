package Manager;

import sun.misc.Signal;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

public class Manager {
    private static Executor executor;
    private static  ServerSocket server;
    private BiFunction<Integer, Integer, Integer> bitwiseOperation;
    public ProcessBuilder buildFProcess;
    public ProcessBuilder buildGProcess;
    private static Process fProcess;
    private static Process gProcess;
    private static  Integer value;
    private static List<Runnable> processTasks;
    private static  List<Integer> ProcessesResults;
    private static Boolean fProcessFaild;
    private static Boolean gProcessFailed;
    private static  AtomicBoolean cancel;
    private static Integer twoOrLessCompucations;
    private static  int softFailCounterF;
    private static  int softFailCounterG;
    Manager(Integer inputValue) throws IOException {
        value = inputValue;

        processTasks = new ArrayList<>(
                List.of(() -> {
                            try {
                                fProcess = buildFProcess.start();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        },
                        () -> {
                            try {
                                gProcess =  buildGProcess.start();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                ));
        ProcessesResults = new ArrayList<>();
        bitwiseOperation = (x, y) -> x * y;

        server = new ServerSocket(1009);
        executor = Executors.newFixedThreadPool(2);

        String classPath = Objects.requireNonNull(Manager.class.getClassLoader().getResource(".")).toString();
        buildFProcess = new ProcessBuilder("java", "-cp", classPath, "funcClients.FProcess");
        buildGProcess = new ProcessBuilder("java", "-cp", classPath, "funcClients.GProcess");

        softFailCounterF = 0;
        softFailCounterG = 0;
        twoOrLessCompucations = 2;
        fProcessFaild = false;
        gProcessFailed = false;
        cancel = new AtomicBoolean(false);
    }

    private static void initSignalHandler(){
        Signal.handle(new Signal("INT"), signal -> cancelMenuIfSignal());
    }

    public  void startComputing(){
        initSignalHandler();

        for(Runnable task: processTasks) {
            executor.execute(task);
        }
        sendValueToProcesses();
        while (true){
            if(!fProcess.isAlive() && !gProcess.isAlive()){
                readResults();
                if(twoOrLessCompucations == 0) {
                    break;
                } else if(cancel.get()) {
                    break;
                } else {
                    sendValueToProcesses();
                }
            } else if(cancel.get()) {
                break;
            }
        }
    }


    private static void cancelMenuIfSignal(){
        System.out.println("Please confirm that computation should be stopped by y or n");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true){
            try {
                if (reader.ready()){
                    String s = "";
                    try {
                        s = reader.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(s.equals("y")){
                        cancel.set(true);
                        return;
                    } else if (s.equals("n")) {
                        initSignalHandler();
                        return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendValueToProcesses(){
        for(int i = 0; i < twoOrLessCompucations; i++)
        {
            try (
                    Socket socket = server.accept();
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())
            ){
                out.writeObject(value);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void readResults(){
        int temp = twoOrLessCompucations;
        for(int i = 0; i < twoOrLessCompucations; i++) {
            try (Socket socket = server.accept()) {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                if(cancel.get()){
                    return;
                }
                String res = (String) in.readObject();
                String processIndex = res.substring(0, 1);
                res = res.substring(1);

                if(res.equals("hard")){
                    if(processIndex.equals("f")) {
                        fProcessFaild = true;
                    } else {
                        gProcessFailed = true;
                    }
                    System.out.println(processIndex + " hard failed");
                    temp--;
                } else if (res.equals("soft")){
                    if(processIndex.equals("f")) {
                        if(softFailCounterF < 5) {
                            executor.execute(processTasks.get(0));
                            softFailCounterF++;
                            System.out.println(processIndex + " soft failed");
                        } else {
                            fProcessFaild = true;
                            System.out.println(processIndex + " hard failed");
                            temp--;
                        }
                    } else {
                        if(softFailCounterG < 5) {
                            executor.execute(processTasks.get(1));
                            softFailCounterG++;
                            System.out.println(processIndex + " soft failed");
                        } else {
                            gProcessFailed = true;
                            System.out.println(processIndex + " hard failed");
                            temp--;
                        }
                    }
                } else {
                    ProcessesResults.add(Integer.parseInt(res));
                    temp--;
                }
            } catch (IOException | ClassNotFoundException e ) {
                System.out.println(e);
            }
        }
        twoOrLessCompucations = temp;
    }

    public Integer getFProcessStatus(){
        return fProcessFaild ? -1 : fProcess.isAlive() ? softFailCounterF + 1 : 0;
    }
    public Integer getGProcessStatus(){
        return gProcessFailed ? -1 : gProcess.isAlive() ? softFailCounterG + 1 : 0;
    }

    public Boolean getIfCancell(){
        return cancel.get();
    }

    public Integer getResult(){
        return bitwiseOperation.apply(ProcessesResults.get(0), ProcessesResults.get(1));
    }
}
