package main.requests;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import main.requests.json_templates.InitialRequest;
import main.requests.json_templates.InitialResponse;
import main.user.User;
import main.user.UserRepository;

import java.util.Date;

@Controller    // This means that this class is a Controller
@RequestMapping(path="/request") // This means URL's start with /demo (after main.Application path)
public class RequestController {
    @Autowired
    private RequestRepository requestRepository;
    @Autowired
    private UserRepository userRepository;


    @RequestMapping(value = "/new", method = RequestMethod.POST , produces = "application/json")
    public @ResponseBody
    InitialResponse addNewUser (@RequestBody InitialRequest initialRequest) {
        InitialResponse rs = new InitialResponse();//initialise
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

            //TODO get Latitide and longgitude of the closest driver
            String latitude = "";
            String longitude= "";

            //TODO this once we have the driver details
            rs.setStatus("Success");
            rs.setRequestId(String.valueOf(newRequest.getId()));
            rs.setDriverName(driver.getUserName());
            rs.setVehicleRegistration(driver.getVehicleRegistration());
            rs.setDriverPhone(driver.getUserPhone());
            rs.setDriverLat(latitude);
            rs.setDriverLong(longitude);

        }
        else//fake user request
        {
            rs.setStatus("Error");
            rs.setMessage("Customer does not exist");

        }
        return rs;
    }

    @GetMapping(path="/all")
    public @ResponseBody Iterable<Request> getAllUsers() {
        // This returns a JSON or XML with the main.requests
        return requestRepository.findAll();
    }
}
