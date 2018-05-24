package main.requests;

import com.google.maps.model.LatLng;
import com.pusher.rest.Pusher;
import main.requests.json_templates.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import main.user.User;
import main.user.UserRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@Controller    // This means that this class is a Controller
@RequestMapping(path="/request") // This means URL's start with /demo (after main.Application path)
public class RequestController {
    @Autowired
    private RequestRepository requestRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private  RequestRejectionRepository requestRejectionRepository;
    @Autowired
    private RequestCancellationRepository requestCancellationRepository;

    public final String PUSHER_APPID= "507480";
    public final String PUSHER_KEY= "830d3e455fd9cfbcec39";
    public final String PUSHER_SECRET= "896c5ab9b5d25bebcad2";
    public final String PUSHER_CLUSTER= "ap2";
    public final boolean PUSHER_ENCRYPTION= true;
    public final double DISTANCE_THRESHOLD = 500000.0;//TODO change this to an appropiate figure

    //Fare multipliers
    public final long BASEFARE = 120;
    public final long TIME_MULTIPLIER = 2;
    public final long DISTANCE_MULTIPLIER = 28;


    @RequestMapping(value = "/new", method = RequestMethod.POST , produces = "application/json")
    public @ResponseBody
    StandardResponse newRequest (@RequestBody InitialRequest initialRequest) {
        StandardResponse rs = new StandardResponse();//initialise
        if(userRepository.existsByUserPhone(initialRequest.getUserPhone()))
        {
            Request newRequest = new Request();
            User rider = userRepository.findByUserPhone(initialRequest.getUserPhone());
            newRequest.setUserPhone(initialRequest.getUserPhone());//save the phone number of the requester
            newRequest.setRequestTime(new Date());//set date to now
            newRequest.setSourceLatitude(initialRequest.getSourceLatitude());
            newRequest.setSourceLongitude(initialRequest.getSourceLongitude());
            newRequest.setDestinationLatitude(initialRequest.getDestinationLatitude());
            newRequest.setDestinationLongitude(initialRequest.getDestinationLongitude());
            newRequest.setDestinationDescription(initialRequest.getDestinationDescription());
            newRequest.setSourceDescription(initialRequest.getSourceDescription());
            requestRepository.save(newRequest);

            rider.setCurrentLongitude(initialRequest.getSourceLongitude());
            rider.setCurrentLatitude(initialRequest.getSourceLatitude());
            userRepository.save(rider);

            LatLng customerLocation = new LatLng(Double.parseDouble(rider.getCurrentLatitude()),Double.parseDouble(rider.getCurrentLongitude()));
            User driver = getClosestDriverToCustomer(customerLocation,null);//no exceptions

            if(driver == null)
            {
                rs.setStatus("Error");
                rs.setMessage("Can't find a driver");
                pushNoDriverAvailableMessage(newRequest, rider);
                return rs;
            }

            driver.setRideInProgress(true);
            userRepository.save(driver);

            pushRideRequestMessage(newRequest,driver, rider);//Send out push to driver to accept ride

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
            driver.setCurrentLatitude(initialRequest.getDriverLatitude());
            driver.setCurrentLongitude(initialRequest.getDriverLongitude());
            userRepository.save(driver);//update driver location

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

            //Log the rejection
            RequestRejection requestRejection = new RequestRejection();
            requestRejection.setRequestID(request.getId());
            requestRejection.setDriverPhone(request.getDriverPhone());
            requestRejection.setRejectionTime(new Date());

            requestRejectionRepository.save(requestRejection);

            User driver = userRepository.findByUserPhone(request.getDriverPhone());
            driver.setRideInProgress(false);
            userRepository.save(driver);

            User rider = userRepository.findByUserPhone(request.getUserPhone());
            LatLng customerLocation = new LatLng(Double.parseDouble(rider.getCurrentLatitude()),Double.parseDouble(rider.getCurrentLongitude()));
            ArrayList<User> driverExceptions = new ArrayList<User>();

            Iterable<RequestRejection> requestRejections = requestRejectionRepository.findAllByRequestID(request.getId());

            for (RequestRejection rejection: requestRejections) {
                User rejectedDriver = userRepository.findByUserPhone(rejection.getDriverPhone());
                driverExceptions.add(rejectedDriver);
            }

            driver = getClosestDriverToCustomer(customerLocation,driverExceptions);

            if(driver != null){
                driver.setRideInProgress(true);
                userRepository.save(driver);

                pushRideRequestMessage(request,driver,rider);//send out new request
            }
            else{
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

    @RequestMapping(value = "/driver/location/update", method = RequestMethod.POST , produces = "application/json")
    public @ResponseBody
    StandardResponse updateDriverLocation (@RequestBody DriverLocationUpdate initialRequest) {
        StandardResponse rs = new StandardResponse();//initialise
        //long id = ;
        if(initialRequest.getRequestId().equals("UPDATE"))
        {
            User driver = userRepository.findByUserPhone(initialRequest.getDriverPhone());
            driver.setCurrentLatitude(initialRequest.getDriverLatitude());
            driver.setCurrentLongitude(initialRequest.getDriverLongitude());
            userRepository.save(driver);

            //send ack
            rs.setStatus("Success");
            rs.setMessage("Request Successfully made");
        }
        else if(requestRepository.existsById(Long.valueOf(Integer.parseInt(initialRequest.getRequestId())))){
            Request request = (requestRepository.findById(Long.valueOf(initialRequest.getRequestId()))).get() ;
            //TODO check if driver null
            User driver = userRepository.findByUserPhone(request.getDriverPhone());
            driver.setCurrentLatitude(initialRequest.getDriverLatitude());
            driver.setCurrentLongitude(initialRequest.getDriverLongitude());
            userRepository.save(driver);

            pushDriverLocationUpdateToRider(request,driver);

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

    @RequestMapping(value = "/driver/availability", method = RequestMethod.POST , produces = "application/json")
    public @ResponseBody
    StandardResponse setDriverAvailability (@RequestBody AvailabilityStatus initialRequest) {
        StandardResponse rs = new StandardResponse();//initialise

        User driver = userRepository.findByUserPhone(initialRequest.getDriverPhone());
        if(initialRequest.getAvailability().equals("true"))
        {
            driver.setAvailability(true);
        }
        else{
            driver.setAvailability(false);
        }
        userRepository.save(driver);
        
        rs.setStatus("Success");
        rs.setStatus("Driver Availability has been updated");
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

    @RequestMapping(value = "/complete", method = RequestMethod.POST , produces = "application/json")
    public @ResponseBody
    StandardResponse completeRide (@RequestBody RideComplete initialRequest) {//Driver will start the ride
        StandardResponse rs = new StandardResponse();//initialise
        long id = Integer.parseInt(initialRequest.getRequestId());
        if(requestRepository.existsById(Long.valueOf(id))){
            Request request = (requestRepository.findById(Long.valueOf(initialRequest.getRequestId()))).get() ;
            request.setRideEndTime(new Date());//set time to now
            long minutes = (request.getRideEndTime().getTime()- request.getRideStartTime().getTime())/(60*1000);
            int distance = Integer.parseInt(initialRequest.getDistance());

            //calculate fare
            long cost = BASEFARE + (minutes * TIME_MULTIPLIER) + (distance * DISTANCE_MULTIPLIER);
            request.setRideTime(""+minutes);
            request.setCost(""+cost);
            request.setDistanceTravelled(initialRequest.getDistance());
            requestRepository.save(request);

            User driver = userRepository.findByUserPhone(request.getDriverPhone());
            driver.setRideInProgress(false);
            userRepository.save(driver);

            User rider = userRepository.findByUserPhone(request.getUserPhone());
            pushRideCompletedMessage(request,rider);

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

    @RequestMapping(value = "/cancel", method = RequestMethod.POST , produces = "application/json")
    public @ResponseBody
    StandardResponse cancelRide (@RequestBody DriverRequestAction initialRequest) {//Driver will start the ride
        StandardResponse rs = new StandardResponse();//initialise
        long id = Integer.parseInt(initialRequest.getRequestId());
        if(requestRepository.existsById(Long.valueOf(id))){
            Request request = (requestRepository.findById(Long.valueOf(initialRequest.getRequestId()))).get() ;

            if(request.getDriverPhone() != null){
                User driver = userRepository.findByUserPhone(request.getDriverPhone());
                driver.setRideInProgress(false);
                userRepository.save(driver);
            }

            User user = userRepository.findByUserPhone(initialRequest.getDriverPhone());//The variable name rephrasing

            if(user != null){
                if(user.getUserType().equals("Driver")){//driver cancelled the request.
                    User rider = userRepository.findByUserPhone(request.getUserPhone());
                    pushRideCancelledMessage(request,user);
                    pushRideCancelledMessage(request,rider);
                }
                else{//its a customer
                    pushRideCancelledMessage(request,user);
                    //send message to driver if one has been assigned
                    if(request.getDriverPhone()!=null){
                        User driver = userRepository.findByUserPhone(request.getDriverPhone());
                        pushRideCancelledMessage(request,driver);
                    }
                }
                request.setRideCancelled(true);
                requestRepository.save(request);

                //cancellation logged in DB
                RequestCancellation requestCancellation = new RequestCancellation();
                requestCancellation.setRequestID(request.getId());
                requestCancellation.setUserPhone(initialRequest.getDriverPhone());//can be user or driver
                requestCancellation.setCancellationTime(new Date());

                requestCancellationRepository.save(requestCancellation);//save

                //send ack
                rs.setStatus("Success");
                rs.setMessage("Request Successfully made");
                rs.setRequestId(initialRequest.getRequestId());
                return rs;
            }

            rs.setStatus("Error");
            rs.setMessage("Invalid User");

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

    private void pushRideCancelledMessage(Request request, User user) {//push to the user passed as parameter
        Pusher pusher = initializePusher();
        HashMap<String,String> rideCancelledMessage = new HashMap<>();
        rideCancelledMessage.put("requestId",String.valueOf(request.getId()));
        rideCancelledMessage.put("status","Success");
        rideCancelledMessage.put("message","Ride Cancelled");
        pusher.trigger(user.getUserPhone(), "ride_cancelled",rideCancelledMessage);
    }

    private void pushRideStartedMessage(Request request, User rider) {
        Pusher pusher = initializePusher();
        HashMap<String,String> rideStartedMessage = new HashMap<>();
        rideStartedMessage.put("requestId",String.valueOf(request.getId()));
        rideStartedMessage.put("status","Success");
        rideStartedMessage.put("message","Ride Started");
        pusher.trigger(rider.getUserPhone(), "ride_started",rideStartedMessage);
    }

    private void pushRideCompletedMessage(Request request, User rider) {
        Pusher pusher = initializePusher();
        HashMap<String,String> rideCompletedMessage = new HashMap<>();
        rideCompletedMessage.put("requestId",String.valueOf(request.getId()));
        rideCompletedMessage.put("status","Success");
        rideCompletedMessage.put("message","Ride Completed");
        rideCompletedMessage.put("cost",request.getCost());
        rideCompletedMessage.put("rideTime",request.getRideTime());
        rideCompletedMessage.put("distance",request.getDistanceTravelled());

        //push to both guys
        pusher.trigger(rider.getUserPhone(), "ride_completed",rideCompletedMessage);
        pusher.trigger(request.getDriverPhone(), "ride_completed",rideCompletedMessage);
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

    private void pushRideRequestMessage(Request request, User driver, User rider){
        //send message to driver about request
        Pusher pusher = initializePusher();
        HashMap<String,String> driverRequest = new HashMap<>();
        driverRequest.put("requestId",String.valueOf(request.getId()));
        driverRequest.put("status","Success");
        driverRequest.put("riderPhone",request.getUserPhone());
        driverRequest.put("riderName", rider.getUserName());
        driverRequest.put("sourceLatitude",request.getSourceLatitude());
        driverRequest.put("destinationLatitude",request.getDestinationLatitude());
        driverRequest.put("sourceLongitude",request.getSourceLongitude());
        driverRequest.put("destinationLongitude",request.getDestinationLatitude());
        driverRequest.put("destinationDescription",request.getDestinationDescription());
        driverRequest.put("sourceDescription",request.getSourceDescription());
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
        driverRequest.put("driverLatitude",driver.getCurrentLatitude());
        driverRequest.put("driverLongitude",driver.getCurrentLongitude());
        pusher.trigger(request.getUserPhone(), "driver_accepted",driverRequest);
    }

    private void pushDriverLocationUpdateToRider(Request request, User driver) {
        Pusher pusher = initializePusher();
        HashMap<String,String> driverLocationUpdate = new HashMap<>();
        driverLocationUpdate.put("requestId",String.valueOf(request.getId()));
        driverLocationUpdate.put("status","Success");
        driverLocationUpdate.put("message","Driver Location Updated");
        driverLocationUpdate.put("driverLatitude",driver.getCurrentLatitude());
        driverLocationUpdate.put("driverLongitude",driver.getCurrentLongitude());
        pusher.trigger(request.getUserPhone(), "driver_location_updated",driverLocationUpdate);
    }

    private User getClosestDriverToCustomer(LatLng customerLocation, ArrayList<User> exceptions) {
        Iterable <User> availableDrivers = userRepository.findAllByAvailability(true);
        for (User possibleDriver : availableDrivers) {
            LatLng driverLocation = new LatLng(Double.parseDouble(possibleDriver.getCurrentLatitude()),Double.parseDouble(possibleDriver.getCurrentLongitude()));
            double distanceToCustomer = distance(driverLocation.lat,driverLocation.lng,customerLocation.lat,customerLocation.lng);
            if(distanceToCustomer < DISTANCE_THRESHOLD && (!possibleDriver.getRideInProgress())){
                if(exceptions == null)
                    return possibleDriver;
                else if(!exceptions.contains(possibleDriver))
                    return possibleDriver;
            }
        }
        return null;
    }

    private static double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        //convert to kilometers
        dist = dist * 1.609344;

        return (dist);
    }

    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }
}
