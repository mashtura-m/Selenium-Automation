package current_scripts

class gmail_userName_validator {
    //find a valid gmail username grom given input, create a test case table using the output
  //GMAIL USERNAME Rule: https://support.google.com/mail/answer/9211434?hl=en
    static void main(String[] args) throws IOException {
        //6-30 chars long
        String rulePattern1 = "^[A-Za-z0-9\\.?]{6,30}@";
        //Usernames cannot contain an ampersand (&), equals sign (=), underscore (_), apostrophe ('), dash (-), plus sign (+), comma (,), brackets (<,>), or more than one period (.) in a row.
        String rulePattern2 = "[&=_'-\\+,<>]";
        String rulePattern2_1 = "\\.{2,}"
        //Usernames can begin or end with non-alphanumeric characters except periods (.)
        String rulePattern3 = "^[^\\.]";
        //@gmail.com
        String rulePattern4 = "@gmail\\.com";
        //Take User Input
        File input = new File("F:\\Scrapian\\emailInput.txt");
        BufferedReader br = new BufferedReader(new FileReader(input));
        String lines;
        FileWriter outPut = new FileWriter(new File("F:\\Scrapian\\testCase.txt"));
        outPut.write("Test Case #\t\tInput\t\tActual RESULT\\Expected\\Pass/Fail\n\n");
        int usecaseNo = 0;
        while ((lines = br.readLine()) != null) {
            usecaseNo++;
            def line =lines.trim().split(",");
            String data=line[0]
            String expected=line[1]
            String valid="FAIL"
            boolean result = false;
            boolean rule1 = data =~ rulePattern1
            boolean rule2 = data =~ rulePattern2
            boolean rule2_1 = data =~ rulePattern2_1
            boolean rule3 = data =~ rulePattern3
            boolean rule4 = data =~ rulePattern4
            if ((rule1 && rule3 && rule4) && !(rule2 || rule2_1)) {
                result = true;
            }
            if(result.equals(expected)){
                valid="PASS"
            }
            //System.out.println(("#" + usecaseNo + "\t\t\t" + data + "\t\t\t" + result + "\n"))
            outPut.write("#" + usecaseNo + "\t\t\t" + data + "\t\t\t" + result+"\t\t\t"+expected+"\t\t\t"+valid+ "\n");
        }
        outPut.close();
    }
}
