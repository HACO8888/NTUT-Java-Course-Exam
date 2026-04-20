package Quiz2;
import java.util.Scanner;

public class Program10 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        boolean found = false;
        while (!found) {
            n++;
            int t = n;
            int r = 0;
            while (t > 0) {
                r = r * 10 + (t % 10);
                t /= 10;
            }
            if (n == r) {
                found = true;
            }
        }
        System.out.println(n);
    }
}
