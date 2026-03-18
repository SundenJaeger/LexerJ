import java.util.Scanner;

public class Main {
    static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: <file path>");
            return;
        }

        Scanner scanner = new Scanner(System.in);

        LexerJ lexerJ = new LexerJ(args[0]);
        lexerJ.execute();

        System.out.println("\nPress any key to continue...");
        scanner.nextLine();
    }
}