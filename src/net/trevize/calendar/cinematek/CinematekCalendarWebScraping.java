package net.trevize.calendar.cinematek;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

//import net.trevize.tinker.logging.TTrace;

public class CinematekCalendarWebScraping {
  
  public static String                 CINEMATEK_CALENDAR_URL          = "https://cinematek.be/fr/programme/calendrier";

  public static String                 XPATH_SCREENING_DATE_GROUP      = "//div[matches(@class,'(^|\\W)screening-date-group(\\W|$)')]";
  public static String                 XPATH_SCREENING_DATE            = ".//div[matches(@class,'(^|\\W)screening-date(\\W|$)')]";
  public static String                 XPATH_SCREENING_ELEMENTS        = ".//div[matches(@class,'(^|\\W)screening-elements(\\W|$)')]";
  public static String                 XPATH_SCREENING                 = ".//a[matches(@class,'(^|\\W)screening(\\W|$)')]";
  public static String                 XPATH_SCREENING_TIME            = ".//h4[matches(@class,'(^|\\W)screening__time(\\W|$)')]";
  public static String                 XPATH_FILM_TITLES               = ".//span[matches(@class,'(^|\\W)film__title(\\W|$)')]";
  public static String                 XPATH_SCREENING_ORIGINAL_TITLES = ".//strong";
  public static String                 XPATH_FILM_DETAILS              = ".//span[matches(@class,'(^|\\W)film__details(\\W|$)')]";
  public static String                 XPATH_FILM_DIRECTORS            = ".//span[matches(@class,'(^|\\W)film__directors(\\W|$)')]";
  public static String                 XPATH_FILM_CAST                 = ".//span[matches(@class,'(^|\\W)film__cast(\\W|$)')]";

  private XPathFactory                 fXpathFactory                   = new net.sf.saxon.xpath.XPathFactoryImpl();
  private Map<String, XPathExpression> fXpathMap                       = new HashMap<>();
  
  public enum MyScreeningLocation {
    LEDOUX,
    PLATEAU,
    HÔTELDECLÈVES,
    MUSEE,
    UNKNOWN;
    public static MyScreeningLocation retrieveScreeningLocation( String aScreeningLocationAsString ) {
      MyScreeningLocation l_myScreeningLocation = UNKNOWN;
      if ( ( aScreeningLocationAsString != null ) && ( ! aScreeningLocationAsString.isEmpty() ) ) {
    	  l_myScreeningLocation = MyScreeningLocation.valueOf( aScreeningLocationAsString.toUpperCase() );
    	  if ( l_myScreeningLocation == null ) {
    		  l_myScreeningLocation = UNKNOWN;
    	  }
      }
      return l_myScreeningLocation;
    }
  }
  
  public static String fMyScreeningDateFormatAsString = "yyyy-MM-dd HH:mm";
  public SimpleDateFormat fMyScreeningDateFormat = new SimpleDateFormat( fMyScreeningDateFormatAsString );
  
  public static String fICalendarDateFormatAsString = "yyyyMMdd'T'HHmmssZ";
  public SimpleDateFormat fICalendarDateFormat = new SimpleDateFormat( fICalendarDateFormatAsString );
  
  public class MyScreening {
    public Date                fDate                 = null;
    public MyScreeningLocation fScreeningLocation    = null;
    public String              fFilmOriginalTitles   = null;
    public String              fFilmTitles           = null;
    public String              fFilmDetails          = null;
    public String              fFilmDirectors        = null;
    public String              fFilmCast             = null;
    public String              fCinematekWebsiteLink = null;
  }
  public TreeMap<Date, MyScreening> fAllScreeningsMap = new TreeMap<>();
  
  public XPathExpression retrieveXpathExpression( String aXpath ) {
    XPathExpression l_xpathExpression = fXpathMap.get( aXpath );
    if ( l_xpathExpression == null ) {
      try {
        XPath l_screeningDateGroupXpath = fXpathFactory.newXPath();
        l_xpathExpression = l_screeningDateGroupXpath.compile( aXpath );
        fXpathMap.put( aXpath, l_xpathExpression );
      }
      catch ( XPathExpressionException l_exception ) {
        l_exception.printStackTrace();
      }
    }
    return l_xpathExpression;
  }
  
  public void retrieveCinematekCalendarAsXmlFile() {
    //TTrace.info( this, "retrieveCinematekCalendarAsXmlFile" );
    try {
      Date l_retrievalDate = new Date();
      WebClient l_webClient = new WebClient();
      l_webClient.getOptions().setUseInsecureSSL( true );
      l_webClient.getOptions().setCssEnabled( false );
      l_webClient.getOptions().setJavaScriptEnabled( false );
      l_webClient.getOptions().setUseInsecureSSL( true );
      l_webClient.getOptions().setCssEnabled( false );
      l_webClient.getOptions().setJavaScriptEnabled( false );
      HtmlPage l_htmlPage = l_webClient.getPage( CINEMATEK_CALENDAR_URL );
      FileWriter l_fileWriter = new FileWriter( new File( "Web-CINEMATEK-calendrier.xml" ) );
      l_fileWriter.write( l_htmlPage.asXml() );
      l_fileWriter.close();
      l_htmlPage.cleanUp();
    }
    catch ( FailingHttpStatusCodeException | IOException l_exception ) {
      l_exception.printStackTrace();
    }
  }
  
  public void extractCalendar() {
	  try {
		  DocumentBuilderFactory l_documentBuilderFactory = DocumentBuilderFactory.newInstance();
		  DocumentBuilder l_documentBuilder = l_documentBuilderFactory.newDocumentBuilder();
		  Document l_document = l_documentBuilder.parse( new FileInputStream( new File( "Web-CINEMATEK-calendrier.xml" ) ) );

		  NodeList l_screeningDateGroupNodeList = (NodeList) retrieveXpathExpression( XPATH_SCREENING_DATE_GROUP ).evaluate( l_document, XPathConstants.NODESET );
		  for (int l_screeningDateGroupNodeIndex = 0; l_screeningDateGroupNodeIndex < l_screeningDateGroupNodeList.getLength(); ++l_screeningDateGroupNodeIndex ) {
			  try {
				  Node l_screeningDateGroupNode = l_screeningDateGroupNodeList.item( l_screeningDateGroupNodeIndex );

				  // We should have 1 screening-date and 1 screening-elements per screening-date-group
				  NodeList l_screeningDateNodeList = (NodeList) retrieveXpathExpression( XPATH_SCREENING_DATE ).evaluate( l_screeningDateGroupNode, XPathConstants.NODESET );
				  NodeList l_screeningElementsNodeList = (NodeList) retrieveXpathExpression( XPATH_SCREENING_ELEMENTS ).evaluate( l_screeningDateGroupNode, XPathConstants.NODESET );

				  // Extract screening date
				  Node l_screeningDateNode = l_screeningDateNodeList.item( 0 );
				  NamedNodeMap l_screeningDateAttributes = l_screeningDateNode.getAttributes();
				  Node l_dataDateAttributeNode = l_screeningDateAttributes.getNamedItem( "data-date" );
				  String l_screeningDateAsString = l_dataDateAttributeNode.getNodeValue();

				  // Iterate over the screenings
				  Node l_screeningElementsNode = l_screeningElementsNodeList.item( 0 );
				  NodeList l_screeningNodeList = (NodeList) retrieveXpathExpression( XPATH_SCREENING ).evaluate( l_screeningElementsNode, XPathConstants.NODESET );
				  for ( int l_screeningNodeIndex = 0; l_screeningNodeIndex < l_screeningNodeList.getLength(); ++l_screeningNodeIndex ) {
					  Node l_screeningNode = l_screeningNodeList.item( l_screeningNodeIndex );
					  NamedNodeMap l_screeningAttributes = l_screeningNode.getAttributes();
					  try {
						  // Extract the time
						  Node l_screeningTimeNode = (Node) retrieveXpathExpression( XPATH_SCREENING_TIME ).evaluate( l_screeningNode, XPathConstants.NODE );
						  String l_screeningTime = l_screeningTimeNode.getTextContent().replaceAll( "[^0-9:]", "" );

						  // Extract the location
						  Node l_screeningLocationNamedItem = l_screeningAttributes.getNamedItem( "data-location" );
						  String l_screeningLocation = "";
						  if ( l_screeningLocationNamedItem != null ) {
							  l_screeningLocation = l_screeningLocationNamedItem.getNodeValue();
						  }

						  // Extract link to cinematek website
						  Node l_screeningHrefNamedItem = l_screeningAttributes.getNamedItem( "href" );
						  String l_cinematekWebsiteLink = "";
						  if ( l_screeningHrefNamedItem != null ) {
							  l_cinematekWebsiteLink = l_screeningHrefNamedItem.getNodeValue();
						  }

						  // Extract film titles
						  Node l_filmTitlesNode = (Node) retrieveXpathExpression( XPATH_FILM_TITLES ).evaluate( l_screeningNode, XPathConstants.NODE );
						  String l_filmTitles = "";
						  if ( l_filmTitlesNode != null ) {
							  l_filmTitles = l_filmTitlesNode.getTextContent().replaceAll( "\\n", "" ).replaceAll( " +", " " );
						  }

						  // Extract original titles
						  Node l_filmOriginalTitlesNode = (Node) retrieveXpathExpression( XPATH_SCREENING_ORIGINAL_TITLES ).evaluate( l_filmTitlesNode, XPathConstants.NODE );
						  String l_filmOriginalTitles = "";
						  if ( l_filmOriginalTitlesNode != null ) {
							  l_filmOriginalTitles = l_filmOriginalTitlesNode.getTextContent().replaceAll( "\\n", "" ).replaceAll( " +", " " );
						  }
						  if ( l_filmOriginalTitles.isEmpty() ) {
							  l_filmOriginalTitles = l_filmTitles;
						  }

						  // Extract details
						  Node l_filmDetailsNode = (Node) retrieveXpathExpression( XPATH_FILM_DETAILS ).evaluate( l_screeningNode, XPathConstants.NODE );
						  String l_filmDetails = "";
						  if ( l_filmDetailsNode != null ) {
							  l_filmDetails = l_filmDetailsNode.getTextContent().replaceAll( "\\n", "" ).replaceAll( " +", " " );
						  }

						  // Extract film director
						  Node l_filmDirectorsNode = (Node) retrieveXpathExpression( XPATH_FILM_DIRECTORS ).evaluate( l_screeningNode, XPathConstants.NODE );
						  String l_filmDirectors = "";
						  if ( l_filmDirectorsNode != null ) {
							  l_filmDirectors = l_filmDirectorsNode.getTextContent().replaceAll( "\\n", "" ).replaceAll( " +", " " );
						  }

						  // Extract film cast
						  Node l_filmCastNode = (Node) retrieveXpathExpression( XPATH_FILM_CAST ).evaluate( l_screeningNode, XPathConstants.NODE );
						  String l_filmCast = "";
						  if ( l_filmCastNode != null ) {
							  l_filmCast = l_filmCastNode.getTextContent().replaceAll( "\\n", "" ).replaceAll( " +", " " );
						  }

						  // Add MyScreening
						  MyScreening l_myScreening = new MyScreening();
						  try {
							  l_myScreening.fDate = fMyScreeningDateFormat.parse( String.format( "%s %s", l_screeningDateAsString, l_screeningTime ) );
						  }
						  catch ( ParseException l_exception ) {
							  l_exception.printStackTrace();
						  }

						  //          if ( l_myScreening.fDate.before( new Date( System.currentTimeMillis() - ( System.currentTimeMillis() % 86400 ) ) ) ) {
						  //        	  continue;
						  //          }

						  l_myScreening.fScreeningLocation = MyScreeningLocation.retrieveScreeningLocation( l_screeningLocation );
						  l_myScreening.fFilmOriginalTitles = l_filmOriginalTitles.trim();
						  l_myScreening.fFilmTitles = l_filmTitles.trim();
						  l_myScreening.fFilmDetails = l_filmDetails.trim().replaceAll( "^[^a-zA-Z1-9]+", "" ).replaceAll( "[^a-zA-Z1-9]+$", "" );
						  l_myScreening.fFilmDirectors = l_filmDirectors.trim();
						  l_myScreening.fFilmCast = l_filmCast.trim();
						  l_myScreening.fCinematekWebsiteLink = l_cinematekWebsiteLink.trim();
						  fAllScreeningsMap.put( l_myScreening.fDate, l_myScreening );
					  }
					  catch ( Exception l_exception ) {
						  l_exception.printStackTrace();
						  if ( l_screeningNode != null ) {
							  StringWriter writer = new StringWriter();
							  Transformer transformer = TransformerFactory.newInstance().newTransformer();
							  transformer.transform(new DOMSource(l_screeningNode), new StreamResult(writer));
							  String xml = writer.toString();
							  System.err.println( xml );
						  }
					  }
				  }
			  }
			  catch ( Exception l_exception ) {
				  l_exception.printStackTrace();
			  }
		  }
	  }
	  catch ( XPathExpressionException | DOMException | ParserConfigurationException | SAXException | IOException l_exception ) {
		  l_exception.printStackTrace();
	  }
  }
  
  public void writeCalendarIcsFile() {
    StringBuilder l_iCalendarStringBuilder = new StringBuilder();
    l_iCalendarStringBuilder.append( "BEGIN:VCALENDAR\n" );
    l_iCalendarStringBuilder.append( "PRODID:-//trevize.net//NONSGML CINEMATEK calendar//EN\n" );
    l_iCalendarStringBuilder.append( "VERSION:2.0\n" );
    l_iCalendarStringBuilder.append( "X-WR-TIMEZONE:Europe/Brussels\n" );
    
    for ( Date l_screeningDate : fAllScreeningsMap.keySet() ) {
      CinematekCalendarWebScraping.MyScreening l_myScreening = fAllScreeningsMap.get( l_screeningDate );
      l_iCalendarStringBuilder.append( "BEGIN:VEVENT\n" );
      l_iCalendarStringBuilder.append( String.format( "DTSTART:%s\n", fICalendarDateFormat.format( l_screeningDate ) ) );
      l_iCalendarStringBuilder.append( String.format( "DTEND:%s\n" , fICalendarDateFormat.format( l_screeningDate ) ) );
      l_iCalendarStringBuilder.append( String.format( "DTSTAMP:%s\n", fICalendarDateFormat.format( new Date() ) ) );
      l_iCalendarStringBuilder.append( String.format( "UID:%s\n", UUID.randomUUID() ) );
      l_iCalendarStringBuilder.append( String.format( "SUMMARY:%s\n", l_myScreening.fFilmOriginalTitles ) );
      l_iCalendarStringBuilder.append( String.format( "DESCRIPTION:%s\\nDirectors: %s\\nCast: %s\\n%s\\n%s\n",
        l_myScreening.fFilmTitles,
        l_myScreening.fFilmDirectors,
        l_myScreening.fFilmCast,
        String.format( "[%s] %s", l_myScreening.fScreeningLocation, l_myScreening.fFilmDetails ),
        l_myScreening.fCinematekWebsiteLink ) );
      l_iCalendarStringBuilder.append( "END:VEVENT\n" );
    }
    
    l_iCalendarStringBuilder.append( "END:VCALENDAR\n" );
    
    // Write iCal file
    try {
      FileWriter l_fileWriter = new FileWriter( new File( "CINEMATEK.ics" ) );
      l_fileWriter.write( l_iCalendarStringBuilder.toString() );
      l_fileWriter.close();
    }
    catch ( IOException l_exception ) {
      l_exception.printStackTrace();
    }
  }
  
  public static void main(String args[]) throws Exception {
    CinematekCalendarWebScraping l_cinematekCalendarScrapping = new CinematekCalendarWebScraping();
    l_cinematekCalendarScrapping.retrieveCinematekCalendarAsXmlFile();
    l_cinematekCalendarScrapping.extractCalendar();
    l_cinematekCalendarScrapping.writeCalendarIcsFile();
  }
  
}
