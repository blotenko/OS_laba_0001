package Manager;

import java.io.IOException;
import java.util.Scanner;

 class ServerTHread {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int number = 0;
        System.out.println(" X :");
        number = sc.nextInt();
        try {
            Manager manager = new Manager(number);
            System.out.println("Manager started");
            manager.startComputing();

            Integer fStatus = manager.getFProcessStatus();
            Integer gStatus = manager.getGProcessStatus();
            Boolean cancelStatus = manager.getIfCancell();
            if(cancelStatus){
                if(fStatus > 0){
                    System.out.println("f process did not finished. Number of soft fails: " + (fStatus - 1));
                }
                if(gStatus > 0){
                    System.out.println("g process did not finished. Number of soft fails: " + (fStatus - 1));
                }
            }
            if(fStatus + gStatus == -2) {
                System.out.println("Expression value: failed");
            } else if(fStatus == 0 && gStatus == 0) {
                System.out.println("Expression value: " + manager.getResult());
            } else {
                System.out.println("Expression value: undetermined");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

}
