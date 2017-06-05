package com.wixsite.jingmacv.model;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

/*
 * A web scraper class with one public function called scrape().
 */
public class IndeedScraper extends WebScraper {
	
	// The main function of this class which scrapes a website for vacancy data.
	public static String scrape(String jobTitleInput, String locationInput) {
		// Resets the data in the jbs_vacancy table.
		DBConnection.resetTable();
		// Initializes a web driver with a website.
		init("https://www.indeed.nl/?r=us");
		// Finds and fills in input fields with a job title and location.
		fillInSearchTerms(jobTitleInput, locationInput);
		// Loops over all pages and saves the job title, company and location in the database.
		String status = findAndSaveAllVacancies();
		// Close browser.
		driver.close();
		return status;
	}
	
	private static void fillInSearchTerms(String jobTitleInput, String locationInput) {
		// Finds and fills in input field "what".
		WebElement input = driver.findElement(By.cssSelector("#what"));
		input.sendKeys(jobTitleInput);
		// Finds and fills in input field "where".
		input = driver.findElement(By.cssSelector("#where"));
		input.sendKeys(locationInput);
		// Finds and submits the web form.
		WebElement submit = driver.findElement(By.cssSelector(".input_submit"));
		submit.click();
	}
	
	private static String findAndSaveAllVacancies() {
		String status = null;
		int pageCount = 0;
		// Loops over all pages and saves the job title, company and location in the database.
		while (true) {
			pageCount++;
			// To prevent loading vacancies of the previous page.
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// Retrieves a list of vacancies, each containing a job title, company and location.
			List<WebElement> list = driver.findElements(By.cssSelector(".result"));
			// Loops over the vacancies.
			for (WebElement item : list) {
				WebElement jobTitleTag = null;
				WebElement companyTag = null;
				WebElement locationTag = null;
				String jobTitle = null;
				String company = null;
				String location = null;
				// Finds the job title, company and location per vacancy.
				if (item.findElements(By.cssSelector("#resultsCol .jobtitle")).size() != 0) {
					jobTitleTag = item.findElement(By.cssSelector("#resultsCol .jobtitle"));
					jobTitle = jobTitleTag.getText();
				}
				if (item.findElements(By.cssSelector("#resultsCol .company")).size() != 0) {
					companyTag = item.findElement(By.cssSelector("#resultsCol .company"));
					company = companyTag.getText();
				}
				if (item.findElements(By.cssSelector("#resultsCol .location")).size() != 0) {
					locationTag = item.findElement(By.cssSelector("#resultsCol .location"));
					location = locationTag.getText();
				}
				// If the company is unknown the user would still want to apply to the job.
				if (jobTitle != null && location != null)
					// Saves the vacancies in the database.
					status = DBConnection.saveVacancy(jobTitle, company, location + ", " + pageCount);
					if (status.equals("noData"))
						break;
				
			}
			if (status.equals("noData") || driver.findElements(By.partialLinkText("Volgende")).size() == 0)
				break;
			// Clicks on "next" and on overlays.
			clickNext();
		}
		return status;
	}
	
	private static void clickNext() {
		// If there's a "next" button, it'll be clicked.
		if (driver.findElements(By.partialLinkText("Volgende")).size() != 0)
			retryClick(By.partialLinkText("Volgende"));
		// If there's an overlay, it'll be clicked.
		if (driver.findElements(By.cssSelector("#popover-close-link")).size() != 0)
			driver.findElement(By.cssSelector("#popover-close-link")).click();
		// If there's a cookie message, it'll be clicked.
		if (driver.findElements(By.cssSelector("#cookie-alert-ok")).size() != 0)
			driver.findElement(By.cssSelector("#cookie-alert-ok")).click();		
	}
	
	private static boolean retryClick(By by) {
        boolean result = false;
        int attempts = 0;
        while (attempts < 2) {
            try {
                driver.findElement(by).click();
                result = true;
                break;
            } catch (StaleElementReferenceException e) {
            	e.printStackTrace();
            }
            attempts++;
        }
        return result;
	}
}
