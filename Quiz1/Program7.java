package Quiz1;

import java.util.Scanner;

public class Program7 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int d = 2;

        while (n > 1) {
            if (n % d == 0) {
                System.out.print(d + " ");
                n /= d;
            } else {
                d++;
            }
        }
        System.out.println();
    }
}