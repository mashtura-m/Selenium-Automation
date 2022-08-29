import com.opencsv.CSVWriter
import groovy.transform.Field

import java.time.LocalDate

/* Notes
* For the given url the data comes from the network url
* The response is a json value
* I have used regex to parse the entity detail from response
* I have used OPEN CSV library to dump the data into CSV
* */
String givenUrl = "https://www.interpol.int/How-we-work/Notices/View-Red-Notices"
def html = invoke(givenUrl)
@Field
def countryMap = [:] //Mapping the Country abbriviation to CountryName
def ccR = html =~ /option value="(\w{2})">([^<]+)/
while (ccR.find()) {
    def code = ccR.group(1)
    def country = ccR.group(2)
    countryMap.put(code, country)
}

@Field
String filePath = "F:\\Scrapian\\output.csv"
@Field
File file = new File(filePath);
@Field
FileWriter outputfile = new FileWriter(file)
@Field
int i=1

countryMap.each { key, val ->
    def network_url = "https://ws-public.interpol.int/notices/v1/red?&nationality=$key"
    jsonData = invoke(network_url).toString()
    jsonData = jsonData.replaceAll(/\s+\n/, "\n").trim()
    captureData(jsonData, val)
    //pagination --> Finding the next page link
    while (jsonData=~/(?ism)next\"\W+href":/){
        def nextR=jsonData=~/(?ism)next\"\W+href":"([^"]+)/
        if(nextR.find()) {
            def nextPageUrl = nextR.group(1)
            jsonData = invoke(nextPageUrl)
            captureData(jsonData, val)
        }
    }
    //debug Purpose
    // throw new Exception(">>>>>>>>>>>>>")
}
println("TOTAL ENTITIES: $i")

def captureData(String source, def country) {
    // requirement
    def name, dob, height, weight, nationality, eyecolor, pob = null, charges = null
    def entityUrlMatcher = source =~ /"self"\W+"href":"([^"]+\d{5,})"/
    while (entityUrlMatcher.find()) {
        def ent_url = entityUrlMatcher.group(1).trim()
        print("COUNTRY: $country ")
        def html = invoke(ent_url)
        def nameRegex = html =~ /forename":"([^"]+).+?name":"(.+?)"/
        if (nameRegex.find()) {
            name = nameRegex.group(2) + " " + nameRegex.group(1)
        }
        def dobRegex = html =~ /date_of_birth":"([^"]+)/
        if (dobRegex.find()) {
            dob = dobRegex.group(1)
        }
        def age=calcAge()
        def chargeMatch = html =~ /charge":"([^"]+)/
        if (chargeMatch.find()) {
            charges = cleanData(chargeMatch.group(1))
        }
        def pobMatch = html =~ /place_of_birth":"([^"]+)/
        if (pobMatch.find()) {
            pob = pobMatch.group(1) + "," + country
        }else {
            pob=country
        }
        def hr = html =~ /(?i)Height":([^\,]+)/
        if (hr.find()) {
            height = hr.group(1) + "Meters"
        }
        def wr = html =~ /(?i)Weight":([^\,]+)/
        if (wr.find()) {
            weight = wr.group(1) + "Kilograms"
        }
        def er = html =~ /eyes_colors_id":([^\,]+)/
        if (er.find()) {
            eyecolor = er.group(1)
            println(eyecolor)
        }
        def nationR = html =~ /(?i)nationalities":([^\,]+)/
        if (nationR.find()) {
            nationality = nationR.group(1)
            println(nationality)
        }
        createEntity(name, dob, height, weight, nationality, eyecolor, pob, charges)
    }

}

def invoke(def url) {
    try {
        println("INVOKING: $url")
        return url.toURL().text
    }catch(Exception e){
        println("<<<<< COULD NOT INVOKE $url\nError Message: $e.message >>>>>")
    }

}

def cleanData(def data) {
    return data.replaceAll(/\r\n/, "\n")
}

def createEntity(def name, def dob, def height, def weight, def nationality, def eyecolor, def pob, def charges) {
    //this method will dump data to CSV
    writeToCSV(name, dob, height, weight, nationality, eyecolor, pob, charges)
    i++
}

def writeToCSV(String... args) {
    try {
        String[] header;
        if (i == 1) {
            header = ["Name", "Date Of Birth", "Height", "Weight", "Nationality", "Eye Color", "Place Of Birth", "Charges"]
            i++
        }
        CSVWriter writer = new CSVWriter(outputfile);
        writer.writeNext(header)
        // adding header to csv
        writer.writeNext(args);
        writer.flush()
        //writer.close();
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}

def calcAge() {
    def today=LocalDate.now()


}
