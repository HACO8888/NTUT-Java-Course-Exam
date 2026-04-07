package Quiz1;

import java.util.Scanner;

public class Program8 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int t = n;
        int r = 0;

        if (n < 0) {
            System.out.println("NO");
        } else {
            while (t != 0) {
                int rem = t % 10;
                r = r * 10 + rem;
                t /= 10;
            }

            if (n == r) {
                System.out.println("YES");
            } else {
                System.out.println("NO");
            }
        }
    }
}