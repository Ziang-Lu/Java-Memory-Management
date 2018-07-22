import java.io.Serializable;

/**
 * Simple Employee class to be serialize.
 *
 * Note that for this class to be serializable, it must implement Serializable
 * interface, and all the types of instance fields must also be serializable.
 * If anyone of them is not, it should be declared as "transient".
 *
 * @author Ziang Lu
 */
public class Employee implements Serializable {

    /**
     * Name of this employee.
     */
    private final String name;
    /**
     * SSN of this employee.
     * When serializing this class, instance fields modified by "transient" will
     * be ignored.
     */
    private transient final int ssn;
    /**
     * Phone number of this employee.
     */
    private String number;
    /**
     * Address of this employee.
     */
    private String addr;

    /**
     * Constructor with parameter.
     * @param name name of the employee
     * @ssn ssn SSN of the employee
     */
    public Employee(String name, int ssn) {
        this.name = name;
        this.ssn = ssn;
    }

    /**
     * Accessor of ssn.
     * @return ssn
     */
    public int getSsn() {
        return ssn;
    }

    /**
     * Accessor of number.
     * @return number
     */
    public String getNumber() {
        return number;
    }

    /**
     * Accessor of addr.
     * @return addr
     */
    public String getAddr() {
        return addr;
    }

    /**
     * Mutator of number.
     * @param number number to set
     */
    public void setNumber(String number) {
        this.number = number;
    }

    /**
     * Mutator of addr.
     * @param addr addr to set
     */
    public void setAddr(String addr) {
        this.addr = addr;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(name).append('\n');
        s.append(ssn).append('\n');
        s.append(number).append('\n');
        s.append(addr).append('\n');
        return s.toString();
    }

}
