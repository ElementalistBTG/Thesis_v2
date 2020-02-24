import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;


/**
 * create timer to run every 10 minutes and execute the UpdateDatabase class
 */
@WebListener
public class AppContextListener implements ServletContextListener {
    //starting of tomcat server executes this
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext ctx = servletContextEvent.getServletContext();
        ctx.log("Starting up Thesis New!!!!");
    }
    //closing the tomcat server executes this
    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        ServletContext ctx = servletContextEvent.getServletContext();
        ctx.log("Shutting down Thesis New!");
    }


}
