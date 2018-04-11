package main.requests;

import com.pusher.rest.Pusher;
import main.requests.json_templates.DriverRequestAction;
import main.requests.json_templates.StandardResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import main.requests.json_templates.InitialRequest;
import main.user.User;
import main.user.UserRepository;

import java.util.Date;
import java.util.HashMap;

@Controller    // This means that this class is a Controller
@RequestMapping(path="/request") // This means URL's start with /demo (after main.Application path)
public class RequestController {
    @Autowired
    private RequestRepository requestRepository;
    @Autowired
    private UserRepository userRepository;

    public final String PUSHER_APPID= "397646";
    public final String PUSHER_KEY= "09d58b5d944ada6621ea";
    public final String PUSHER_SECRET= "abb0ba8c99a964cd3d7e";
    public final String PUSHER_CLUSTER= "ap2";
    public final boolean PUSHER_ENCRYPTION= true;

    @RequestMapping(value = "/new", method = RequestMethod.POST , produces = "application/json")
    public @ResponseBody
    StandardResponse newRequest (@RequestBody InitialRequest initialRequest) {
        StandardResponse rs = new StandardResponse();//initialise
        if(userRepository.existsByUserPhone(initialRequest.getUserPhone()))
        {
            Request newRequest = new Request();
            newRequest.setUserPhone(initialRequest.getUserPhone());//save the phone number of the requester
            newRequest.setRequestTime(new Date());//set date to now

            requestRepository.save(newRequest);

            //TODO get longitude and Lattitude of the the requester and search for closest driver on the db
            User driver = userRepository.findByUserPhone("0720844920");//sample driver

            if(driver == null)
            {
                rs.setStatus("Error");
                rs.setMessage("Can't find a driver");
                return rs;
            }

            //send message to driver about request
            Pusher pusher =new Pusher(PUSHER_APPID,PUSHER_KEY,PUSHER_SECRET);
            pusher.setCluster(PUSHER_CLUSTER);
            pusher.setEncrypted(PUSHER_ENCRYPTION);

            HashMap<String,String> driverRequest = new HashMap<>();
            driverRequest.put("requestId",String.valueOf(newRequest.getId()));
            pusher.trigger(driver.getUserPhone(), "ride_request",driverRequest);

            //send ack
            rs.setStatus("Success");
            rs.setMessage("Request Successfully made");
            rs.setRequestId(String.valueOf(newRequest.getId()));

        }
        else//fake user request
        {
            rs.setStatus("Error");
            rs.setMessage("Customer does not exist");

        }
        return rs;
    }

    @RequestMapping(value = "/driver/accept", method = RequestMethod.POST , produces = "application/json")
    public @ResponseBody
    StandardResponse driverRequestResponse (@RequestBody DriverRequestAction initialRequest) {
        StandardResponse rs = new StandardResponse();//initialise
        long id = Integer.parseInt(initialRequest.getRequestId());
        if(requestRepository.existsById(Long.valueOf(id))){
            //TODO send message to client that driver has accepted the order
            Request request = (requestRepository.findById(Long.valueOf(initialRequest.getRequestId()))).get() ;
            request.setDriverPhone(initialRequest.getDriverPhone());
            requestRepository.save(request);

            User driver = userRepository.findByUserPhone(initialRequest.getDriverPhone());

            Pusher pusher =new Pusher(PUSHER_APPID,PUSHER_KEY,PUSHER_SECRET);
            pusher.setCluster(PUSHER_CLUSTER);
            pusher.setEncrypted(PUSHER_ENCRYPTION);

            //push notification to client and driver information
            HashMap<String,String> driverRequest = new HashMap<>();
            driverRequest.put("requestId",initialRequest.getRequestId());
            driverRequest.put("driverName",driver.getUserName());
            driverRequest.put("driverPhone",driver.getUserPhone());
            driverRequest.put("vehicleRegistration",driver.getVehicleRegistration());
            driverRequest.put("latitude","11111");
            driverRequest.put("longitude","1111");
            pusher.trigger(request.getUserPhone(), "driver_accepted",driverRequest);

            //send ack
            rs.setStatus("Success");
            rs.setMessage("Request Successfully made");
            rs.setRequestId(initialRequest.getRequestId());
        }
        else{
            rs.setStatus("Error");
            rs.setMessage("Invalid Request");
        }

        return rs;
    }


    @GetMapping(path="/all")
    public @ResponseBody Iterable<Request> getAllUsers() {
        // This returns a JSON or XML with the main.requests
        return requestRepository.findAll();
    }
}
