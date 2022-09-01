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
@Field
String genderFilterUrl
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
    print("COUNTRY: $val ")
    def min_age = 20
    def tailUrl
    while (min_age < 100) {
        def max_age_nation = min_age + 5
        println("Start $min_age $max_age_nation")
        def age_nation_filter = "https://ws-public.interpol.int/notices/v1/red?&nationality=$key&ageMin=$min_age&ageMax=$max_age_nation"
        (tailUrl, max_age_nation) = getDataUrl(age_nation_filter, max_age_nation, min_age)
        if (tailUrl) {
            age_nation_filter = "https://ws-public.interpol.int/notices/v1/red?&nationality=$key" + tailUrl
            println("Getting Nationwise $val")
            getPageJson(age_nation_filter, val)
        }
        min_age=max_age_nation+1

    }
min_age=20
while (min_age < 100) {
    def max_age_arrest = min_age + 5
    println("MIN: $min_age Max:$max_age_arrest")
    def age_arrest_filter = "https://ws-public.interpol.int/notices/v1/red?&arrestWarrantCountryId=$key&ageMin=$min_age&ageMax=$max_age_arrest"
    (tailUrl, max_age_arrest) = getDataUrl(age_arrest_filter, max_age_arrest, min_age)
    if (tailUrl) {
        println("Getting Arrestwise $val")
        age_arrest_filter = "https://ws-public.interpol.int/notices/v1/red?&arrestWarrantCountryId=$key" + tailUrl
        getPageJson(age_arrest_filter, null)
      }
    min_age=max_age_arrest+1


}
/*    def country_filter_Url = "https://ws-public.interpol.int/notices/v1/red?&nationality=$key"
    getPageJson(country_filter_Url, val)
    country_filter_Url = "https://ws-public.interpol.int/notices/v1/red?&arrestWarrantCountryId=$key"
    getPageJson(country_filter_Url, null)
    genderFilterUrl = "https://ws-public.interpol.int/notices/v1/red?&nationality=$key&sexId=F"
    getPageJson(genderFilterUrl, val)
    genderFilterUrl = "https://ws-public.interpol.int/notices/v1/red?&nationality=$key&sexId=M"
    getPageJson(genderFilterUrl, val)
    genderFilterUrl = "https://ws-public.interpol.int/notices/v1/red?&arrestWarrantCountryId=$key&sexId=F"
    getPageJson(genderFilterUrl, null)
    genderFilterUrl = "https://ws-public.interpol.int/notices/v1/red?&arrestWarrantCountryId=$key&sexId=M"
    getPageJson(genderFilterUrl, null)*/
//debug Purpose
//throw new Exception(">>>>>>>>>>>>>")
}

def getDataUrl(def url, def max_age, def min_age) {

    def jsonSource = invokeUrl(url).toString()
    int tot = jsonSource.replaceAll(/(?sm)^(.+?"total":)(\d+)(.+?)$/, '$2').toInteger()
    if (tot == 0) {
        return [null, max_age]
    }
    if (tot > 160) {
        max_age = min_age
    } else {
        max_age = min_age + 5
    }
    println("MIN: $min_age Max: $max_age")
    return ["&resultPerPage=160&ageMin=$min_age&ageMax=$max_age", max_age]
}

genderFilterUrl = "https://ws-public.interpol.int/notices/v1/red?&sexId=U"
getPageJson(genderFilterUrl, null)
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
        ent_url = "https://www.interpol.int/How-we-work/Notices/View-Red-Notices#" + entityID
        createEntity(name, dob, height, weight, nations, eyecolor, pob, charges, sex, age, ent_url)
        //test url : https://www.interpol.int/How-we-work/Notices/View-Red-Notices#2009-17220
    }
}

def invokeUrl(def url) {
    try {
        // println("Invoking : $url")
        return context.invoke(["url": url, "cache": true])//url.toURL().text
    } catch (Exception e) {
        Thread.sleep(20000)
        println("<<<<< COULD NOT invoke : $url\nError Message: $e.message >>>>>")
        return context.invoke(["url": url, "cache": true])
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
def createEntity(def name, def dob, def height, def weight, def nations, def eyecolor, def pob, def charges, def sex, def age, def ent_url) {
    //this method will dump data to CSV
    writeToCSV(name, age, sex, dob, height, weight, nations, eyecolor, pob, charges, ent_url)
    total++
    println("Entity NO. $total")
}

def writeToCSV(String... args) {
    try {
        String[] header;
        if (total == 1) {
            header = ["Name", "Age", "Gender", "Date Of Birth", "Height", "Weight", "Nationality", "Eye Color", "Place Of Birth", "Charges", "Debug_url"]
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
            nextPageUrl = nextPageUrl.replaceAll(/=20/, "=160")
            jsonData = invokeUrl(nextPageUrl).toString()
            captureEntityDetails(jsonData, val)
        }
    }
}
