void main(String[] args) {
    if (args.length < 1) {
        System.err.println("Usage: CFPL <file path>");
        return;
    }

    CFPL cfpl = new CFPL(args[0]);
    cfpl.execute();
}