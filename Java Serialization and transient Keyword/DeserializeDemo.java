import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class DeserializeDemo {

    /**
     * Main driver.
     * @param args arguments from command line
     */
    public static void main(String[] args) {
        Employee e = null;

        try {
            FileInputStream fis = new FileInputStream("reyan_ali.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);

            e = (Employee) ois.readObject();

            ois.close();
            fis.close();
        } catch (IOException ei) {
            ei.printStackTrace();
            return;
        } catch (ClassNotFoundException ec) {
            ec.printStackTrace();
        }

        System.out.println("Deserialize Employee object...");
        System.out.println(e);

        // Note that since Employee.ssn is transient, it will not be serialized.
        // Therefore when we deserialize it, we can see that the SSN of the deserialized employee is the default value
        // 0.
    }

}
