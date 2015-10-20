package com.group9.bankofaz.controller;

import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.group9.bankofaz.model.BankAccount;
import com.group9.bankofaz.model.ExternalUser;
import com.group9.bankofaz.model.Users;
import com.group9.bankofaz.service.RegistrationService;

@Controller
@RequestMapping("/registration")
public class RegistrationController {
	@Autowired
	RegistrationService registerService;

	private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
			+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

	private Pattern email_pattern = Pattern.compile(EMAIL_PATTERN);;

	@RequestMapping(method = RequestMethod.GET) 
	public ModelAndView RegistrationPage() {
		return new ModelAndView("registration");
	}

	@RequestMapping(value = "/reg_validate", method = RequestMethod.POST)
	public ModelAndView addUser(HttpServletRequest request) {
		// capture form fields
		String firstName = request.getParameter("First_Name").toString();
		String middleName = request.getParameter("Middle_Name").toString();
		String lastName = request.getParameter("Last_Name").toString();
		String emailId = request.getParameter("Email_Id").toString();
		String password = request.getParameter("password").toString();
		String repassword = request.getParameter("repassword").toString();
		String addressLine1 = request.getParameter("Address1").toString();
		String addressLine2 = request.getParameter("Address2").toString();
		String city = request.getParameter("City").toString();
		String state = request.getParameter("State").toString();
		String zipcode = request.getParameter("Pin_Code").toString();
		String ssn = request.getParameter("SSN").toString();

		// validate input
		StringBuilder errors = new StringBuilder();
		if (!validateField(firstName, 1, 30, false)) {
			errors.append("<li>First Name must not be empty, be between 1-30 characters and not have spaces</li>");
		}
		if (!validateField(middleName, 0, 30, true)) {
			errors.append("<li>Middle Name must not more than 30 characters</li>");
		}
		if (!validateField(lastName, 1, 30, false)) {
			errors.append("<li>Last Name must not be empty, be between 1-30 characters and not have spaces</li>");
		}

		// email validations
		if (!validateField(emailId, 1, 30, false)) {
			errors.append("<li>Email Id must not be empty, be between 1-30 characters and not have spaces</li>");
		}
		Matcher matcher = email_pattern.matcher(emailId);
		if (!matcher.matches()) {
			errors.append("<li>Email Id must be a proper email address</li>");
		}

		if (registerService.userIfExists(emailId) != null) {
			errors.append("<li>An user exists with the given email, please use an alternate email</li>");
		}

		// password validations
		if (!validateField(password, 1, 30, false)) {
			errors.append("<li>Password must not be empty, be between 1-30 characters and not have spaces</li>");
		} else {
			if (!password.equals(repassword))
				errors.append("<li>Password and Re-entered password are not the same</li>");
		}

		if (!validateField(addressLine1, 1, 30, true)) {
			errors.append("<li>Address Line 1 must not be empty, be between 1-30 characters</li>");
		}
		if (!validateField(addressLine2, 1, 30, true)) {
			errors.append("<li>Address Line 2 must not be empty, be between 1-30 characters</li>");
		}
		if (!validateField(city, 1, 16, true)) {
			errors.append("<li>City must not be empty, be between 1-16 characters and not have spaces</li>");
		}
		if (!validateField(state, 1, 16, false)) {
			errors.append("<li>State must not be empty, be between 1-16 characters and not have spaces</li>");
		}
		if (!validateField(zipcode, 1, 5, false)) {
			errors.append("<li>Zipcode must not be empty, be between 1-5 characters and not have spaces</li>");
		}
		if (!validateField(ssn, 9, 9, false)) {
			errors.append("<li>SSN must not be empty, be 9 characters long and not have spaces</li>");
		}

		if (errors.length() != 0) { // return back with errors and previously
									// inputed values
			Map<String, Object> fieldMap = new HashMap<String, Object>();
			fieldMap.put("firstName", firstName);
			fieldMap.put("lastName", lastName);
			fieldMap.put("middleName", middleName);
			fieldMap.put("emailId", emailId);
			fieldMap.put("password", password);
			fieldMap.put("addressLine1", addressLine1);
			fieldMap.put("addressLine2", addressLine2);
			fieldMap.put("city", city);
			fieldMap.put("state", state);
			fieldMap.put("zipcode", zipcode);
			fieldMap.put("ssn", ssn);

			errors.insert(0, "Please fix the following input errors: <br /><ol>");
			errors.append("</ol>");
			fieldMap.put("errors", errors.toString());
			return new ModelAndView("registration", fieldMap);
		}

		// passed validation, register user
		ExternalUser external = new ExternalUser();
		external.setFirstname(firstName);
		if (middleName != null)
			external.setMiddlename(middleName);

		external.setLastname(lastName);
		external.setAddressline1(addressLine1);
		external.setAddressline2(addressLine2);
		external.setCity(city);
		external.setSsn(ssn);
		external.setState(state);
		external.setUsertype("individual");
		external.setZipcode(zipcode);

		Users users = new Users();
		users.setUsername(emailId);
		users.setEnabled(1);

		StandardPasswordEncoder encryption = new StandardPasswordEncoder();
		users.setPassword(encryption.encode(request.getParameter("password").toString()));
		users.setAuthority("ROLE_INDIVIDUAL");

		external.setEmail(users);

		registerService.addLoginInfo(users);
		PrivateKey key = registerService.addExternalUser(external);

		// user is created now create checking and savings bank accounts
		// for that user
		BankAccount checking = new BankAccount();
		checking.setAccno(registerService.userIfExists(emailId).getUserid() + "01");
		checking.setAcctype("checking");
		checking.setAccStatus("active");
		checking.setBalance(100);
		checking.setOpendate(new Date());
		checking.setUserid(registerService.userIfExists(emailId));

		BankAccount savings = new BankAccount();
		savings.setAccno(registerService.userIfExists(emailId).getUserid() + "02");
		savings.setAcctype("savings");
		savings.setAccStatus("active");
		savings.setBalance(100);
		savings.setOpendate(new Date());
		savings.setUserid(registerService.userIfExists(emailId));

		registerService.addBankAccount(checking);
		registerService.addBankAccount(savings);

		// prepare to pass data back to registration successful page
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("firstName", external.getFirstname());
		map.put("lastName", external.getLastname());
		map.put("showEmailId", emailId);
		map.put("checkingAccountNo", checking.getAccno());
		map.put("savingsAccountNo", savings.getAccno());
		map.put("privateKey", Arrays.toString(key.getEncoded()));

		return new ModelAndView("registrationSuccessful", map);
	}

	private boolean validateField(String field, int minSize, int maxSize, boolean spacesAllowed) {
		if (field == null)
			return false;
		if (!spacesAllowed && field.indexOf(" ") != -1)
			return false;
		if (field.length() < minSize || field.length() > maxSize)
			return false;

		return true;
	}

}