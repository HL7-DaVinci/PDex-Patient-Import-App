/**
 * Loads the immunizations JSON data and renders to table.
 * @returns
 */

document.mamd = {}
document.mamd.immunizations = {}
document.mamd.medication = {}
document.mamd.encounters = {}
document.mamd.providers = {}

function loadImmunizations() {
	readJSONFile("sample_data/immunization.json", function(text) {
	    document.mamd.immunizations = JSON.parse(text);
	
	// document.mamd.immunizations.entry[0].resource.vaccineCode.text
	// document.mamd.immunizations.entry[0].resource.date
	// document.mamd.immunizations.entry[0].resource.wasNotGiven
	
		var tblImmu = document.getElementById("tblImmu");
		for(var i=0; i<document.mamd.immunizations.entry.length; i++) {
			var row = document.createElement("tr");
			tblImmu.appendChild(row);
			
			var _text = document.createElement("td");
			_text.innerHTML = document.mamd.immunizations.entry[i].resource.vaccineCode.text;
			_text.setAttribute("style", "text-align: left");
			row.appendChild(_text);
			
			var _given = document.createElement("td");
			_given.innerHTML = document.mamd.immunizations.entry[i].resource.wasNotGiven;
			row.appendChild(_given);
			
			var _date = document.createElement("td");
			_date.innerHTML = formatDate(document.mamd.immunizations.entry[i].resource.date);
			_date.setAttribute("style", "text-align: right");
			row.appendChild(_date);
		}
	});
}

function loadMedication() {
	readJSONFile("sample_data/medication.json", function(text) {
	    document.mamd.medication = JSON.parse(text);
	    
	    // document.mamd.medication.entry[0].resource.product.form.coding[0].display
	    // document.mamd.medication.entry[0].resource.code.text
	
		var tblImmu = document.getElementById("tblMeds");
		for(var i=0; i<document.mamd.medication.entry.length; i++) {
			var row = document.createElement("tr");
			tblImmu.appendChild(row);
			
			var _given = document.createElement("td");
			_given.innerHTML = document.mamd.medication.entry[i].resource.code.text;
			_given.setAttribute("style", "text-align: left");
			row.appendChild(_given);
			
			var _text = document.createElement("td");
			_text.innerHTML = document.mamd.medication.entry[i].resource.product.form.coding[0].display;
			_text.setAttribute("style", "text-align: right");
			row.appendChild(_text);
			
		}
	});
}

function loadEncounters() {
	readJSONFile("sample_data/encounter.json", function(text) {
	    document.mamd.encounters = JSON.parse(text);
	    
	    // document.mamd.encounters.entry[0].resource.period.start
	    // document.mamd.encounters.entry[0].resource.status
	
		var tblImmu = document.getElementById("tblEnc");
		for(var i=0; i<document.mamd.encounters.entry.length; i++) {
			var row = document.createElement("tr");
			tblImmu.appendChild(row);
			
			var _given = document.createElement("td");
			_given.innerHTML = formatDate(document.mamd.encounters.entry[i].resource.period.start);
			_given.setAttribute("style", "text-align: left");
			row.appendChild(_given);
			
			var _text = document.createElement("td");
			_text.innerHTML = document.mamd.encounters.entry[i].resource.status;
			_text.setAttribute("style", "text-align: right");
			row.appendChild(_text);
			
		}
		
	});
}

function loadProviders() {
	readJSONFile("sample_data/providers.json", function(text) {
	    document.mamd.providers = JSON.parse(text);
	    filterProviderList();
	});
}

function filterProviderList() {
	var tblProv = document.getElementById("tblProviderList");
	var filter = document.getElementById("filterField").value.toUpperCase();
	
	console.log("filtering provider list: " + filter);

	tblProv.innerHTML = "";
	for(var i=0; i<document.mamd.providers.length; i++) {
		
		if(document.mamd.providers[i].name.toUpperCase().indexOf(filter) == -1) continue;
		
		var row = document.createElement("tr");
		tblProv.appendChild(row);
		
		var _given = document.createElement("td");
		_given.innerHTML = "<img src=\"" + document.mamd.providers[i].logo + "\" style=\"height:40px\"/>";
		_given.setAttribute("style", "width: 260px");
		row.appendChild(_given);
		
		var _text = document.createElement("td");
		_text.innerHTML = document.mamd.providers[i].name;
		row.appendChild(_text);
		
	}
	
}

function formatDate(_dateString) {
	var _date = new Date(_dateString);
	return _date.toString();
}

function readJSONFile(file, callback) {
    var rawFile = new XMLHttpRequest();
    rawFile.overrideMimeType("application/json");
    rawFile.open("GET", file, true);
    rawFile.onreadystatechange = function() {
        if (rawFile.readyState === 4 && rawFile.status == "200") {
            callback(rawFile.responseText);
        }
    }
    rawFile.send(null);
}
