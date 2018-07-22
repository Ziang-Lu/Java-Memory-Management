import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class SerializeDemo {

    /**
     * Main driver.
     * @param args arguments from command line
     */
    public static void main(String[] args) {
        Employee e = new Employee("Reyan Ali", 111222333);
        e.setNumber("Some number");
        e.setAddr("Somewhere");

        System.out.println("Serialize Employee object...");
        System.out.println(e);

        try {
            FileOutputStream fos = new FileOutputStream("reyan_ali.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(e);

            oos.close();
            fos.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
