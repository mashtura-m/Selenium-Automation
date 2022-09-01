import com.opencsv.CSVWriter
import groovy.transform.Field

import java.time.LocalDate
import java.time.Period

/* Notes
* For the given url the data comes from the network url
* The response is a json value
* I have used regex to parse the entity detail from response
* I have used OPEN CSV library to dump the data into CSV
* */
@Field
String givenUrl = "https://www.interpol.int/How-we-work/Notices/View-Red-Notices"
@Field
def countryMap = [:] //Mapping the Country abbriviation to CountryName
@Field
def colorMap = [:]
@Field
String filePath = "F:\\Automation_Testing\\interpol_output.csv"
@Field
File file = new File(filePath);
@Field
FileWriter outputfile = new FileWriter(file)
@Field
int total = 1
@Field
def entityID = []
/*
* Do Mapping for color code and Country code references 
* from given url
* */

def createMap() {
    def jsonData = invokeUrl(givenUrl)
    def ccR = jsonData =~ /option value="(\w{2})">([^<]+)/
    while (ccR.find()) {
        def code = ccR.group(1)
        def country = ccR.group(2)
        countryMap.put(code, country)
    }
    println(countryMap)
    def colorReg = jsonData =~ /data-references='([^>]+)/
    if (colorReg.find()) {
        def desc = colorReg.group(1).replaceAll(/&quot;/, "").trim()
        def regex = desc =~ /eyes:\{([^\}]+)/
        if (regex.find()) {
            def colorCodes = regex.group(1).split(",")
            colorCodes.each { data ->
                def code = data.split(":")[0]
                def colorName = data.split(":")[1]
                colorMap.put(code, colorName)
            }
        }
    }
}

createMap()
//traverse pages using "nationatity", "Gender" and "Wanted by" filters
countryMap.each { key, val ->
    def country_filter_Url = "https://ws-public.interpol.int/notices/v1/red?&nationality=$key"
    getPageJson(country_filter_Url, val)
    country_filter_Url = "https://ws-public.interpol.int/notices/v1/red?&arrestWarrantCountryId=$key"
    getPageJson(country_filter_Url, null)
    String genderFilterUrl = "https://ws-public.interpol.int/notices/v1/red?&nationality=$key&sexId=F"
    getPageJson(genderFilterUrl, val)
    genderFilterUrl = "https://ws-public.interpol.int/notices/v1/red?&nationality=$key&sexId=M"
    getPageJson(genderFilterUrl, val)
    genderFilterUrl = "https://ws-public.interpol.int/notices/v1/red?&arrestWarrantCountryId=$key&sexId=F"
    getPageJson(genderFilterUrl, null)
    genderFilterUrl = "https://ws-public.interpol.int/notices/v1/red?&arrestWarrantCountryId=$key&sexId=M"
    getPageJson(genderFilterUrl, null)
    genderFilterUrl = "https://ws-public.interpol.int/notices/v1/red?&sexId=U"
    getPageJson(genderFilterUrl, null)
    //debug Purpose
    // throw new Exception(">>>>>>>>>>>>>")
}
println("Total Entities: $total")

/**
 * This method will invoke the entityUrls and parse details form json response using REGEX 
 */
def captureEntityDetails(String source, def country) {
    // requirement
    def name, dob, height, weight, nationality, eyecolor, pob = null, charges = null, age
    def entityUrlMatcher = source =~ /"self"\W+"href":"([^"]+-\d{2,})"/
    while (entityUrlMatcher.find()) {
        def ent_url = entityUrlMatcher.group(1).trim()
        //removed dupe entities using the Entity ID that is the last part of the entity Url
        def ID = ent_url.replaceAll(/^(\D+.+?)([^\/]+)$/, '$2')
        if (entityID.contains(ID)) {
            continue
        } else {
            entityID.add(ID)
        }
        //invokeUrl
        def jsonData = invokeUrl(ent_url)
        def nameRegex = jsonData =~ /forename":"([^"]+).+?name":"(.+?)"/
        if (nameRegex.find()) {
            name = nameRegex.group(2) + " " + nameRegex.group(1)
        }

        def dobRegex = jsonData =~ /date_of_birth":"([^"]+)/
        if (dobRegex.find()) {
            dob = dobRegex.group(1).replaceAll(/\//, "-").trim()
        }
        age = convert_DOB_to_Age(dob)
        def chargeMatch = jsonData =~ /charge":"([^"]+)/
        if (chargeMatch.find()) {
            charges = cleanData(chargeMatch.group(1))
        }
        def pobMatch = jsonData =~ /place_of_birth":"([^"]+)/
        def cobMatch = jsonData =~ /country_of_birth_id":"([^"]+)/
        if (pobMatch.find() && cobMatch.find()) {
            pob = pobMatch.group(1) + "," + countryMap.get(cobMatch.group(1))
        } else if (cobMatch.find()) {
            pob = countryMap.get(cobMatch.group(1))
        }
        def hr = jsonData =~ /(?i)Height":([^\,]+)/
        if (hr.find()) {
            height = hr.group(1).replaceAll(/0|null/, "")
            if (height != "") {
                height = height + " Meters"
            }
        }
        def wr = jsonData =~ /(?i)Weight":([^\,]+)/
        if (wr.find()) {
            weight = wr.group(1).replaceAll(/0|null/, "")
            if (weight != "") {
                weight = weight + " Kilograms"
            }
        }
        def er = jsonData =~ /eyes_colors_id":([^\,]+)/
        if (er.find()) {
            eyecolor = er.group(1).replaceAll(/\[|\]|"|null/, "").trim()
            if (eyecolor != "") {
                eyecolor = colorMap.get(eyecolor)
            }
        }
        def sex
        def sexR = jsonData =~ /sex_id":"(\w)"/
        if (sexR.find()) {
            sex = sexR.group(1).replaceAll(/"/, "").trim()
        }
        def nationR = jsonData =~ /(?i)nationalities":(.+?\])\,/
        def nationList, nations = ""
        if (nationR.find()) {
            nationality = nationR.group(1).replaceAll(/\[|\]|null/, "").trim()
            nationList = nationality.split(",")
            int len = nationList.size()
            nationList.each { val ->
                val = val.replaceAll(/"/, "").trim()
                if (len > 1) {
                    nations += countryMap.get(val) + ","
                } else {
                    nations += countryMap.get(val)
                }
                len--
            }
        }
        createEntity(name, dob, height, weight, nations, eyecolor, pob, charges, sex, age)
    }
}

def invokeUrl(def url) {
    try {
        println("Invoking : $url")
        return url.toURL().text
    } catch (Exception e) {
        println("<<<<< COULD NOT invoke : $url\nError Message: $e.message >>>>>")
    }

}

def cleanData(def data) {
    data = data.replaceAll(/^\W+/, "").trim()
    data = data.replaceAll(/\/r|\/n/, ";")
    data = data.replaceAll("\\r\\n", ";").trim()
    return data.replaceAll(/(?s)\s+/, " ")
}
/**
 * Multiple details for the same column are stored in a comma separated way
 *
 * */
def createEntity(def name, def dob, def height, def weight, def nations, def eyecolor, def pob, def charges, def sex, def age) {
    //this method will dump data to CSV
    writeToCSV(name, age, sex, dob, height, weight, nations, eyecolor, pob, charges)
    total++
}

def writeToCSV(String... args) {
    try {
        String[] header;
        if (total == 1) {
            header = ["Name", "Age", "Gender", "Date Of Birth", "Height", "Weight", "Nationality", "Eye Color", "Place Of Birth", "Charges"]
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

def convert_DOB_to_Age(def date) {
    //yyyy-mm-dd
    def DOB
    def dates = date.split("-")
    if (dates.size() > 1) {
        DOB = new LocalDate(dates[0].toInteger(), dates[1].toInteger(), date[2].toInteger())
    } else {
        DOB = new LocalDate(dates[0].toInteger(), 0, 0)
    }
    LocalDate today = LocalDate.now()
    def age = Period.between(DOB, today).getYears()
    return age.toString()
}

/*
* The following method will handle pagination
* */

def getPageJson(String country_filter_Url, def val) {
    def jsonData = invokeUrl(country_filter_Url).toString()
    jsonData = jsonData.replaceAll(/\s+\n/, "\n").trim()
    captureEntityDetails(jsonData, val)
    //pagination --> Finding the next page link
    while (jsonData =~ /(?ism)next\"\W+href":/) {
        def nextR = jsonData =~ /(?ism)next\"\W+href":"([^"]+)/
        if (nextR.find()) {
            def nextPageUrl = nextR.group(1)
            jsonData = invokeUrl(nextPageUrl).toString()
            captureEntityDetails(jsonData, val)
        }
    }
}
