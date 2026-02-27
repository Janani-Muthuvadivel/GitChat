document.getElementById("loginBtn").addEventListener("click", function () {

    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    fetch("/i18-process-manager/LoginServlet", {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "username=" + encodeURIComponent(username) +
              "&password=" + encodeURIComponent(password)
    }).then(response =>response.json())
	.then(data=>{
		if(data.status=="success"){
			localStorage.setItem("vendor_id", data.vendor_id);
			window.location.href = "VendorPage.html";
		}
		else{
			alert("Invalid Username or Password");
		}
	});

});

document.addEventListener("keydown",function (e){
	if(e.key === "Enter"){
		document.getElementById("loginBtn").click();
	}
})
