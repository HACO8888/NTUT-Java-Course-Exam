package Quiz2;
import java.util.Scanner;

public class Program9 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        if (n == 0) {
            System.out.println(0);
        } else {
            while (n > 0) {
                System.out.print(n % 2);
                n /= 2;1
            }
            System.out.println();
        }
    }
}
