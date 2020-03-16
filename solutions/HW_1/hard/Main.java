public class Main {

    public static void main(String[] args) {
        RecursiveWalk walk = new RecursiveWalk();
        try {
            walk.run(args);
        } catch (MyException e) {
            e.getMessage();
        }
    }
}
