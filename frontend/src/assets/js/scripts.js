function showAvatarForm() {
  document.getElementById('avatarForm').style.display = "block";
}

function passwordCompare() {
  let password = document.getElementById("password")
    , confirm_password = document.getElementById("confirm_password");

  function validatePassword(){
    if(password.value !== confirm_password.value) {
      confirm_password.setCustomValidity("Passwords Don't Match");
    } else {
      confirm_password.setCustomValidity('');
    }
  }

  password.onchange = validatePassword;
  confirm_password.onkeyup = validatePassword;
}

let check = function () {
  if (document.getElementById('password').value === document.getElementById('confirm_password').value) {
    document.getElementById('message').style.display = "none"
    document.getElementById('submit_register').disabled = false;
  } else {
    document.getElementById('message').style.display = "inline"
    document.getElementById('message').style.color = 'red';
    document.getElementById('message').innerHTML = 'Niepoprawne hasło';
    document.getElementById('submit_register').disabled = false;
  }
};


$(document).ready(function () {
  $imgSrc = $('#imgProfile').attr('src');
  function readURL(input) {

    if (input.files && input.files[0]) {
      var reader = new FileReader();

      reader.onload = function (e) {
        $('#imgProfile').attr('src', e.target.result);
      };

      reader.readAsDataURL(input.files[0]);
    }
  }
  $('#btnChangePicture').on('click', function () {
    // document.getElementById('profilePicture').click();
    if (!$('#btnChangePicture').hasClass('changing')) {
      $('#profilePicture').click();
    }
    else {
      // change
    }
  });
  $('#profilePicture').on('change', function () {
    readURL(this);
    $('#btnChangePicture').addClass('changing');
    $('#btnChangePicture').attr('value', 'Confirm');
    $('#btnDiscard').removeClass('d-none');
    // $('#imgProfile').attr('src', '');
  });
  $('#btnDiscard').on('click', function () {
    // if ($('#btnDiscard').hasClass('d-none')) {
    $('#btnChangePicture').removeClass('changing');
    $('#btnChangePicture').attr('value', 'Change');
    $('#btnDiscard').addClass('d-none');
    $('#imgProfile').attr('src', $imgSrc);
    $('#profilePicture').val('');
    // }
  });
});
