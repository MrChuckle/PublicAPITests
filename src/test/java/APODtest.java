import io.restassured.http.ContentType;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class APODtest {
    URL url;
    {
        try {
            url = new URL("https://api.nasa.gov/planetary/apod");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    String key = "pxdXhkK91IwfK5GX9maWBM3ZWRwsawEhF7K1tdYA";
    String invalidKey = "INVALIDKEYwfK5GX9maWBM3ZWRwsawEhF7K1tdYA";
    LocalDate today = LocalDate.now();

    @Test
    public void accessWithValidAPIKeySuccessful() {
        given().param("api_key", key).
                when().
                    get(url).
                then().
                    assertThat().statusCode(200).
                and().
                    contentType(ContentType.JSON);
    }

    @Test
    public void accessWithInvalidAPIKeyUnsuccessful() {
        given().param("api_key", invalidKey).
                when().
                    get(url).
                then().
                    assertThat().statusCode(403);
    }

    @Test
    public void defaultAPIKeyIsSuccessful() {
        given().
                when().
                    get(url).
                then().
                    assertThat().statusCode(200).
                and().
                    contentType(ContentType.JSON);
    }

    @Test
    public void defaultDateIsToday() {
        given().param("api_key", key).
                when().
                    get(url).
                then().
                    assertThat().body("date", equalTo(today.toString()));
    }

    @DataProvider(name="invalidDateFormatProvider")
    public Object[][] invalidDateFormatProvider() {
        return new Object[][] {{"string"},
                {1},
                {"14-08-2020"},
                {"10000-01-01"},
                {"2020/08/14"},
                {true}};
    }

    @Test(dataProvider = "invalidDateFormatProvider")
    public void invalidDateFormatsReturnErrorMessage(Object invalidDateFormat) {
        given().param("api_key", key).and().param("date", invalidDateFormat).
                when().
                    get(url).
                then().
                    assertThat().statusCode(400).
                and().
                    contentType(ContentType.JSON).
                and().
                    body("msg", equalTo("time data '" + invalidDateFormat + "' does not match format '%Y-%m-%d'"));
    }

    @DataProvider(name="invalidDateProvider")
    public Object[][] invalidDateProvider() {
        return new Object[][] {{"0001-01-01"},
                {"1995-06-14"},
                {today.plusDays(1).toString()},
                {"9999-12-31"}};
    }

    @Test(dataProvider = "invalidDateProvider")
    public void invalidDatesReturnErrorMessage(Object invalidDate) {
        String formattedDate = today.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        given().param("api_key", key).and().param("date", invalidDate).
                when().
                    get(url).
                then().
                    assertThat().statusCode(400).
                and().
                    contentType(ContentType.JSON).
                and().
                    body("msg", equalTo("Date must be between Jun 16, 1995 and " + formattedDate + "."));
    }

    @DataProvider(name = "validDateProvider")
    public Object[][] validDateProvider() {
        return new Object[][] {{"1995-06-16"},
                {today.toString()},
                {"2000-06-10"}};
    }

    @Test(dataProvider = "validDateProvider")
    public void validDatesAreSuccessful(String validDate) {
        given().param("api_key", key).and().param("date", validDate).
                when().
                    get(url).
                then().
                    assertThat().statusCode(200).
                and().
                    contentType(ContentType.JSON);
    }

    @Test
    public void rateLimitIsDecreasing() {
        int remainingRateLimit = Integer.parseInt(given().param("api_key", key).when().get(url).then().extract().header("X-RateLimit-Remaining"));
        given().param("api_key", key).
                when().
                    get(url).
                then().
                    assertThat().header("X-RateLimit-Remaining", String.valueOf(remainingRateLimit-1));
    }

    @Test(dataProvider = "validDateProvider")
    public void urlIsALinkToAPicture(String validDate) {
        String imgurl = given().
                param("api_key", key).and().param("date", validDate).
                when().
                    get(url).
                then().
                    extract().path("url");
        given().
                when().
                    get(imgurl).
                then().
                    assertThat().statusCode(200).
                and().
                    contentType(startsWith("image"));
    }

    @Test(dataProvider = "validDateProvider")
    public void hdurlIsALinkToAPicture(String validDate) {
        String imgurl = given().
                param("api_key", key).and().param("date", validDate).and().param("hd", true).
                when().
                    get(url).
                then().
                    extract().path("hdurl");
        given().
                when().
                get(imgurl).
                then().
                assertThat().statusCode(200).
                and().
                contentType(startsWith("image"));
    }

    @Test
    public void defaultHdUrlNotRetrieved() {
        given().param("api_key", key).
                when().
                    get(url).
                then().
                    assertThat().body("hdurl", nullValue());
    }

    @Test
    public void hdUrlRetrievedWhenRequested() {
        given().param("api_key", key).and().param("hd", true).
                when().
                    get(url).
                then().
                    assertThat().body("hdurl", notNullValue());

    }

    @Test
    public void hdUrlNotRetrievedWhenNotRequested() {
        given().param("api_key", key).and().param("hd", false).
                when().
                    get(url).
                then().
                    assertThat().body("hdurl", nullValue());
    }

}
