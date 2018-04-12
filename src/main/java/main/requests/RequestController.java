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

            pushRideRequestMessage(newRequest,driver);//Send out push to driver to accept ride

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
    StandardResponse acceptRideRequest (@RequestBody DriverRequestAction initialRequest) {
        StandardResponse rs = new StandardResponse();//initialise
        long id = Integer.parseInt(initialRequest.getRequestId());
        if(requestRepository.existsById(Long.valueOf(id))){
            Request request = (requestRepository.findById(Long.valueOf(initialRequest.getRequestId()))).get() ;
            request.setDriverPhone(initialRequest.getDriverPhone());
            requestRepository.save(request);

            User driver = userRepository.findByUserPhone(initialRequest.getDriverPhone());

            pushAcceptedRideRequestMessage(request,driver);//send to rider

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

    @RequestMapping(value = "/driver/reject", method = RequestMethod.POST , produces = "application/json")
    public @ResponseBody
    StandardResponse rejectRideRequest (@RequestBody DriverRequestAction initialRequest) {
        StandardResponse rs = new StandardResponse();//initialise
        long id = Integer.parseInt(initialRequest.getRequestId());
        if(requestRepository.existsById(Long.valueOf(id))){
            Request request = (requestRepository.findById(Long.valueOf(initialRequest.getRequestId()))).get() ;
            //TODO log the rejection
            //request.setDriverPhone(initialRequest.getDriverPhone());
            //requestRepository.save(request);

            //TODO find another driver
            User driver = userRepository.findByUserPhone("0720844920");//sample driver

            if(driver != null){
                pushRideRequestMessage(request,driver);//send out new request
            }
            else{
                User rider = userRepository.findByUserPhone(request.getUserPhone());
                pushNoDriverAvailableMessage(request,rider);
            }


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

    @RequestMapping(value = "/startRide", method = RequestMethod.POST , produces = "application/json")
    public @ResponseBody
    StandardResponse startRide (@RequestBody DriverRequestAction initialRequest) {//Driver will start the ride
        StandardResponse rs = new StandardResponse();//initialise
        long id = Integer.parseInt(initialRequest.getRequestId());
        if(requestRepository.existsById(Long.valueOf(id))){
            Request request = (requestRepository.findById(Long.valueOf(initialRequest.getRequestId()))).get() ;
            request.setRideStartTime(new Date());//set time to now
            requestRepository.save(request);

            User rider = userRepository.findByUserPhone(request.getUserPhone());
            pushRideStartedMessage(request,rider);

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

    private Pusher initializePusher(){
        Pusher pusher =new Pusher(PUSHER_APPID,PUSHER_KEY,PUSHER_SECRET);
        pusher.setCluster(PUSHER_CLUSTER);
        pusher.setEncrypted(PUSHER_ENCRYPTION);
        return pusher;
    }

    private void pushRideStartedMessage(Request request, User rider) {
        Pusher pusher = initializePusher();
        HashMap<String,String> rideStartedMessage = new HashMap<>();
        rideStartedMessage.put("requestId",String.valueOf(request.getId()));
        rideStartedMessage.put("status","Success");
        rideStartedMessage.put("message","Ride Started");
        pusher.trigger(rider.getUserPhone(), "ride_started",rideStartedMessage);
    }

    private void pushNoDriverAvailableMessage(Request request, User rider) {
        //send message to driver about request
        Pusher pusher = initializePusher();
        HashMap<String,String> riderInformation = new HashMap<>();
        riderInformation.put("requestId",String.valueOf(request.getId()));
        riderInformation.put("status","Error");
        riderInformation.put("message","No driver Available");
        pusher.trigger(rider.getUserPhone(), "no_driver",riderInformation);
    }

    private void pushRideRequestMessage(Request request, User driver){
        //send message to driver about request
        Pusher pusher = initializePusher();
        //TODO add more information about distation and source of driver
        HashMap<String,String> driverRequest = new HashMap<>();
        driverRequest.put("requestId",String.valueOf(request.getId()));
        driverRequest.put("status","Success");
        pusher.trigger(driver.getUserPhone(), "ride_request",driverRequest);
    }

    private void pushAcceptedRideRequestMessage(Request request, User driver){//destination is rider
        Pusher pusher = initializePusher();

        //push notification to client and driver information
        HashMap<String,String> driverRequest = new HashMap<>();
        driverRequest.put("requestId",String.valueOf(request.getId()));
        driverRequest.put("driverName",driver.getUserName());
        driverRequest.put("driverPhone",driver.getUserPhone());
        driverRequest.put("vehicleRegistration",driver.getVehicleRegistration());
        driverRequest.put("latitude","11111");
        driverRequest.put("longitude","1111");
        pusher.trigger(request.getUserPhone(), "driver_accepted",driverRequest);
    }
}
