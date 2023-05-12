package net.trevize.calendar;

import java.io.File;
import java.util.Date;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

public class ICalendar {

  public static String CALENDAR_BEGIN           = "BEGIN:VCALENDAR";
  public static String CALENDAR_END             = "END:VCALENDAR";
  public static String CALENDAR_PRODID          = "PRODID:";
  public static String CALENDAR_VERSION         = "VERSION:";
  public static String CALENDAR_VERSION_2_0     = ( CALENDAR_VERSION + ":2.0" );
  public static String EVENT_BEGIN              = "BEGIN:VEVENT";
  public static String EVENT_UID                = "UID:";
  public static String EVENT_CREATION_TIMESTAMP = "DTSTAMP:";
  public static String EVENT_START_DATE         = "DTSTART:";
  public static String EVENT_END_DATE           = "DTEND:";
  public static String EVENT_SUMMARY            = "SUMMARY:";
  public static String EVENT_DESCRIPTION        = "DESCRIPTION:";
  public static String EVENT_END                = "END:VEVENT";
  
  public static class ICalendarEvent {
    public String fEventCreationTimestamp = null;
    public String fEventUid               = null;
    public String fEventStartDate         = null;
    public String fEventEndDate           = null;
    public String fEventSummary           = null;
    public String fEventDescription       = null;
    public String fEventFullText          = null;
  }
  
  private TreeMap<Date,Set<ICalendarEvent>> fCalendarEventList = new TreeMap<>();
  
  public void importICalendarFile( File aICalendarFile ) {
    Scanner l_iCalendarScanner = new Scanner( aICalendarFile );
    StringBuilder l_eventStringBuilder = new StringBuilder();
    while ( l_iCalendarScanner.hasNextLine() ) {
      String l_iCalendarNextLine = l_iCalendarScanner.nextLine().trim();
      if ( EVENT_BEGIN.equals( l_iCalendarNextLine ) ) {
        // Extract the event
        l_eventStringBuilder.setLength( 0 );
        l_eventStringBuilder.append( l_iCalendarNextLine + "\n" );
        ICalendarEvent l_iCalendarEvent = new ICalendarEvent();
        while ( l_iCalendarScanner.hasNextLine() ) {
          l_iCalendarNextLine = l_iCalendarScanner.nextLine().trim();
          l_eventStringBuilder.append( l_iCalendarNextLine + "\n" );
          
          if ( l_iCalendarNextLine.startsWith( EVENT_UID ) ) {
            String l_eventStartDate = l_iCalendarNextLine.substring( l_iCalendarNextLine.indexOf( ':' ) );
          }
          else if ( l_iCalendarNextLine.startsWith( EVENT_END_DATE ) ) {
            
          }
          if ( EVENT_END.equals( l_iCalendarNextLine ) ) {
            break;
          }
        }
        
        // Retrieve creation timestamp
        
        
        // Retrieve UID
        
        // Retrieve start date
        
        // Retrieve end date
        
        // Retrieve summary
        
        // Retrieve description 
      }
    }
    l_iCalendarScanner.close();
  }
  
}
