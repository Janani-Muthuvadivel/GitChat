console.log("Vendor Page loaded");

const vendorId = localStorage.getItem("vendor_id");
let fileId = null;
let allData = [];

fetch("/i18-process-manager/DynamicVendorServlet", {
    method: "POST",
    headers: {
        "Content-Type": "application/x-www-form-urlencoded"
    },
    body: "vendor_id=" + encodeURIComponent(vendorId)
})
.then(response => response.json())
.then(data => {
    allData = data;
    renderTable(allData);
    console.log(data);

})
.catch(error => console.error(error));

function renderEmptyTable(){
	
    document.getElementById("dataOfTable").style.display = "none";
	var emptyRecordIndication = document.getElementById("emptyRecordIndication");
    emptyRecordIndication.innerHTML = `
    <div id="noRecordDiv" >No record found</div>
    `;
}

function renderTable(data) {

    /*const tableBody = document.getElementById("tableBody");
    tableBody.innerHTML = "";*/
	
	
	document.getElementById("dataOfTable").style.display = "block";
		var emptyRecordIndication = document.getElementById("emptyRecordIndication");
	    emptyRecordIndication.innerHTML = "";

	    const tableBody = document.getElementById("tableBody");
	    tableBody.innerHTML = "";
	    
	    if(data.length === 0){
			console.log("Empty");
			renderEmptyTable();
			return;
		}
	
	
    data.forEach(item => {

        const row = document.createElement("tr");
		const message = item.message;
		const userName = item.user_name;
        const createdDate = new Date(item.created_at);
        const formattedDate = createdDate.toLocaleString("en-IN", {
            hour: '2-digit',
            minute: '2-digit',
            day: '2-digit',
            month: 'short'
        });

        const languageList = item.languages
            .map(lang => lang.split("[")[0])
            .join(",");

        const statusText = item.status === "Pending"
            ? "Pending"
            : "Settled";

        const uploadClass = item.status === "Pending"
		    ? "uploadButton"
		    : "uploadButton disabled";

        row.innerHTML = `
            <td>${item.file_id}</td>
            <td>${userName}</td>
            <td>${formattedDate}</td>
            <td>${message}</td>
            <td class="fileNameCell">
                <span class="fileName">
                    <i class="fa-solid fa-file"></i>&nbsp; application.properties
                </span>
            </td>
            <td>${languageList}</td>
			<td>
			   <span class="status ${statusText.toLowerCase()}">
			       ${statusText}
			   </span>
			</td>
            <td class="${uploadClass}">
                
				<svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 -960 960 960" width="24px" fill="#1f1f1f"><path d="M440-320v-326L336-542l-56-58 200-200 200 200-56 58-104-104v326h-80ZM240-160q-33 0-56.5-23.5T160-240v-120h80v120h480v-120h80v120q0 33-23.5 56.5T720-160H240Z"/></svg>
            </td>
        `;
		row.dataset.originalHtml = row.innerHTML;

        tableBody.prepend(row);
    });
}


document.getElementById("tableBody")
.addEventListener("click", function(e){

    const row = e.target.closest("tr");
    if (!row) return;

    const currentFileId = row.cells[0].innerText;

    if (e.target.closest(".fileName")) {

        fetch("/i18-process-manager/DownloadFileServlet", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: "fileId=" + encodeURIComponent(currentFileId)
        })
        .then(response => response.blob())
        .then(data => {
            const url = window.URL.createObjectURL(data);
            const a = document.createElement("a");
            a.href = url;
            a.download = "application.properties";
            document.body.appendChild(a);
            a.click();
            a.remove();
            window.URL.revokeObjectURL(url);
        })
        .catch(err => console.error(err));
    }

    else if (e.target.closest(".uploadButton")
            && !e.target.closest(".uploadButton").classList.contains("disabled")) {

        fileId = currentFileId;
        openModal();
    }
});


const fileInput = document.getElementById("fileInput");
const fileLabel = document.getElementById("fileLabel");

fileInput.addEventListener("change", function() {

    if (fileInput.files.length > 0) {
        const fileName = fileInput.files[0].name;

        fileLabel.textContent = fileName;
        document.getElementById("titleText").innerText =
            "File selected successfully";
        document.getElementById("descText").innerText =
            "Selected file: " + fileName;
    } else {
        fileLabel.textContent = "Choose File";
    }
});


function uploadFile() {

    const file = fileInput.files[0];

    if (!file) {
        alert("Please select a file!");
        return;
    }

    if (!fileId) {
        alert("No file selected from table!");
        return;
    }

    const formData = new FormData();
    formData.append("zipFile", file);
    formData.append("fileId", fileId);

    fetch("/i18-process-manager/UploadFileServlet", {
        method: "POST",
        body: formData
    })
    .then(response => response.text())
    .then(result => {
	
	    if(result.includes("FAILED")){
	        alert("File has been submitted!");
			const item = allData.find(d => d.file_id == fileId);
				        if (item) {
				            item.status = "Submitted";
				        }
				
				        applyFilters();
	    } else {
	        alert("File uploaded successfully!");
	
	        const item = allData.find(d => d.file_id == fileId);
	        if (item) {
	            item.status = "Submitted";
	        }
	
	        applyFilters();
	    }
	
	    closeModal();
	})

    .catch(error => {
        console.error("Error uploading file:", error);
    });
}


function goToLogin(){
    window.location.href = "login.html";
}


const statusFilter = document.getElementById("statusFilter");
//const dateFilter = document.getElementById("dateFilter");
const fromDateInput = document.getElementById("fromDate");
const toDateInput = document.getElementById("toDate");

statusFilter.addEventListener("change", applyFilters);
//dateFilter.addEventListener("change", applyFilters);
fromDateInput.addEventListener("change", applyFilters);
toDateInput.addEventListener("change", applyFilters);

/*function applyFilters() {

    let filteredData = [...allData];

    const selectedStatus = statusFilter.value;
    const selectedDate = dateFilter.value;
    
    if (selectedStatus === "Pending") {
	    filteredData = filteredData.filter(item =>
	        item.status === "Pending"
	    );
	}
	
	else if (selectedStatus === "Translated") {
	    filteredData = filteredData.filter(item =>
	        item.status === "Submitted" ||
	        item.status === "Approved" ||
	        item.status === "Rejected"
	    );
	}

    filteredData.sort((a, b) => {
        const dateA = new Date(a.created_at);
        const dateB = new Date(b.created_at);

        if (selectedDate === "NEWEST") {
            return dateB - dateA;
        } else {
            return dateA - dateB;
        }
    });

    renderTable(filteredData);
}*/

function applyFilters() {

    let filteredData = [...allData];

    const selectedStatus = statusFilter.value;
    const fromDateValue = fromDateInput.value;
    const toDateValue = toDateInput.value;

    // Status filter
    if (selectedStatus === "Pending") {
        filteredData = filteredData.filter(item =>
            item.status === "Pending"
        );
    }
    else if (selectedStatus === "Translated") {
        filteredData = filteredData.filter(item =>
            item.status === "Submitted" ||
            item.status === "Approved" ||
            item.status === "Rejected"
        );
    }

    if (fromDateValue && toDateValue) {

        const fromDate = new Date(fromDateValue);
        const toDate = new Date(toDateValue);
        toDate.setHours(23, 59, 59, 999);

        filteredData = filteredData.filter(item => {

            const createdDate = new Date(item.created_at);

            return createdDate >= fromDate &&
                   createdDate <= toDate;
        });
    }

    renderTable(filteredData);
}

const searchInput = document.getElementById("searchInput");
const tableBody = document.getElementById("tableBody");

searchInput.addEventListener("input", function () {

    let searchValue = this.value.toLowerCase();
    let rows = tableBody.getElementsByTagName("tr");

    for (let row of rows) {

        row.innerHTML = row.dataset.originalHtml;

        let text = row.innerText.toLowerCase();

        if (text.includes(searchValue)) {

            row.style.display = "";

            if (searchValue !== "") {
                highlightText(row, searchValue);
            }

        } else {
            row.style.display = "none";
        }
    }
});

function highlightText(element, searchValue) {

    if (!searchValue) return;

    const regex = new RegExp(searchValue, "gi");

    const walker = document.createTreeWalker(
        element,
        NodeFilter.SHOW_TEXT,
        null
    );

    let node;

    while ((node = walker.nextNode())) {

        const text = node.nodeValue;

        if (text.toLowerCase().indexOf(searchValue.toLowerCase()) === -1)
            continue;

        const fragment = document.createDocumentFragment();

        let lastIndex = 0;

        text.replace(regex, (match, index) => {

            fragment.appendChild(
                document.createTextNode(text.slice(lastIndex, index))
            );

            const span = document.createElement("span");
            span.className = "highlight";
            span.textContent = match;

            fragment.appendChild(span);

            lastIndex = index + match.length;
        });

        fragment.appendChild(
            document.createTextNode(text.slice(lastIndex))
        );

        node.parentNode.replaceChild(fragment, node);
    }
}