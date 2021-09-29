import { useState } from "react"
import {
	Button,
	Container,
	CssBaseline,
	Grid,
	Link,
	TextField,
	Typography,
	Box
} from '@material-ui/core';
import Service from "../service/Service"

const SignUp = () => {

	document.title = 'Đăng ký';


	const [username, setUsername] = useState('');
	const [firstname, setFirstname] = useState('');
	const [lastname, setLastname] = useState('');
	const [email, setEmail] = useState('');
	const [password, setPassword] = useState('');
	const [rePassword, setRePassword] = useState('');

	const onUserNameChange = (e) => {
		setUsername(e.target.value);
	};

	const onFirstnameChange = (e) => {
		setFirstname(e.target.value);
	};

	const onLastnameChange = (e) => {
		setLastname(e.target.value);
	}

	const onEmailChange = (e) => {
		setEmail(e.target.value);
	}

	const onPasswordChange = (e) => {
		setPassword(e.target.value);
	};

	const onRePasswordChange = (e) => {
		setRePassword(e.target.value);
	};

	const onSubmit = (e) => {
		e.preventDefault();
		let user = {
			username,
			firstname,
			lastname,
			email,
			password,
		};
		Service.signup(user).then(a => console.log(a));
	}

	return (
		<div>
			<Container component="main" maxWidth="xs">
				<CssBaseline />
				<div className="login-box">
					<Typography component="h1" variant="h5">
						Đăng ký tài khoản
					</Typography>
					<form id="signup-form" className="login-form" noValidate>
						<Grid container spacing={2}>
							<Grid item xs={12}>
								<TextField
									variant="outlined"
									required
									fullWidth
									id="username"
									label="Tên đăng nhập"
									name="username"
									autoComplete="username"
									onChange={onUserNameChange}
								/>
							</Grid>
							<Grid item xs={12} sm={6}>
								<TextField
									autoComplete="fname"
									name="firstName"
									variant="outlined"
									required
									fullWidth
									id="firstName"
									label="Họ"
									autoFocus
									onChange={onFirstnameChange}
								/>
							</Grid>
							<Grid item xs={12} sm={6}>
								<TextField
									variant="outlined"
									required
									fullWidth
									id="lastName"
									label="Tên"
									name="lastName"
									autoComplete="lname"
									onChange={onLastnameChange}
								/>
							</Grid>
							<Grid item xs={12}>
								<TextField
									variant="outlined"
									required
									fullWidth
									id="email"
									label="Địa chỉ email"
									name="email"
									autoComplete="email"
									onChange={onEmailChange}
								/>
							</Grid>
							<Grid item xs={12}>
								<TextField
									variant="outlined"
									required
									fullWidth
									name="password"
									label="Mật khẩu"
									type="password"
									id="password"
									autoComplete="current-password"
									onChange={onPasswordChange}
								/>
							</Grid>
							<Grid item xs={12}>
								<TextField
									variant="outlined"
									required
									fullWidth
									name="rePassword"
									label="Nhập lại mật khẩu"
									type="password"
									id="rePassword"
									autoComplete="current-password"
									onChange={onRePasswordChange}
								/>
							</Grid>
						</Grid>
						<Button
							className="btn-login"
							type="submit"
							fullWidth
							variant="contained"
							color="primary"
							onClick={onSubmit}
						>
							Đăng ký
						</Button>
						<Grid container justify="flex-end">
							<Grid item>
								<Link href="/login" variant="body2">
									Bạn đã có mật khẩu? Đăng nhập.
								</Link>
							</Grid>
						</Grid>
					</form>
				</div>
				<Box mt={5}>
				</Box>
			</Container>
		</div>
	)
}

export default SignUp;